package kitchenpos.application.order;

import kitchenpos.application.ServiceTest;
import kitchenpos.application.order.dto.request.tablegroup.OrderTableIdRequest;
import kitchenpos.application.order.dto.request.tablegroup.TableGroupRequest;
import kitchenpos.domain.order.Order;
import kitchenpos.domain.order.*;
import kitchenpos.domain.order.repository.OrderRepository;
import kitchenpos.domain.order.repository.OrderTableRepository;
import kitchenpos.domain.order.repository.TableGroupRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@SpringBootTest
class TableGroupServiceTest {

    private final TableGroupService tableGroupService;
    private final OrderRepository orderRepository;
    private final OrderTableRepository orderTableRepository;
    private final TableGroupRepository tableGroupRepository;

    @Autowired
    public TableGroupServiceTest(final TableGroupService tableGroupService,
                                 final OrderRepository orderRepository,
                                 final OrderTableRepository orderTableRepository,
                                 final TableGroupRepository tableGroupRepository
    ) {
        this.tableGroupService = tableGroupService;
        this.orderRepository = orderRepository;
        this.orderTableRepository = orderTableRepository;
        this.tableGroupRepository = tableGroupRepository;
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @ServiceTest
    class CreateTest {

        private OrderTable orderTable1;
        private OrderTable orderTable2;

        @BeforeEach
        void setUp() {
            this.orderTable1 = orderTableRepository.save(makeEmptyOrderTable());
            this.orderTable2 = orderTableRepository.save(makeEmptyOrderTable());
        }

        @DisplayName("단체 지정을 한다")
        @Test
        void create() {
            final var request = makeTableGroupRequest(orderTable1.getId(), orderTable2.getId());
            final var actual = tableGroupService.create(request);

            assertThat(actual.getId()).isPositive();
        }

        private TableGroupRequest makeTableGroupRequest(final Long... orderTableId) {
            return new TableGroupRequest(Stream.of(orderTableId)
                    .map(OrderTableIdRequest::new)
                    .collect(Collectors.toUnmodifiableList()));
        }

        @DisplayName("2개 이상의 주문 테이블을 지정해야 한다")
        @Test
        void createWithEmptyOrSingleOrderTable() {
            final var request = makeTableGroupRequest(orderTable1.getId());

            assertThatThrownBy(() -> tableGroupService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("2개 이상의 주문 테이블을 지정해야 합니다.");
        }

        @DisplayName("주문 테이블은 중복되지 않아야 한다")
        @Test
        void createWithDuplicatedOrderTables() {
            final var request = makeTableGroupRequest(orderTable1.getId(), orderTable1.getId());

            assertThatThrownBy(() -> tableGroupService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("중복되는 주문 테이블이 있습니다.");
        }

        @DisplayName("비어있는 주문 테이블이어야 한다")
        @Test
        void createWithNonEmptyOrderTable() {
            final var nonEmptyOrderTable = orderTableRepository.save(makeNonEmptyOrderTable(10));

            final var request = makeTableGroupRequest(orderTable1.getId(), nonEmptyOrderTable.getId());

            assertThatThrownBy(() -> tableGroupService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("비어있지 않은 주문 테이블이 존재합니다.");
        }

        @DisplayName("단체 지정되지 않은 주문 테이블이어야 한다")
        @Test
        void createWithAlreadyGroupAssignedOrderTable() {
            tableGroupRepository.save(new TableGroup(List.of(orderTable1, orderTable2)));

            final var request = makeTableGroupRequest(orderTable1.getId(), orderTable2.getId());

            assertThatThrownBy(() -> tableGroupService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 단체 지정된 주문 테이블입니다.");
        }
    }

    @Nested
    @ServiceTest
    class UngroupTest {

        private OrderTable orderTable1;
        private OrderTable orderTable2;
        private TableGroup tableGroup;

        @BeforeEach
        void setUp() {
            this.orderTable1 = orderTableRepository.save(makeEmptyOrderTable());
            this.orderTable2 = orderTableRepository.save(makeEmptyOrderTable());
            this.tableGroup = tableGroupRepository.save(new TableGroup(List.of(orderTable1, orderTable2)));
        }


        @DisplayName("단체 지정을 해제한다")
        @Test
        void ungroup() {
            final var tableGroupId = tableGroup.getId();

            tableGroupService.ungroup(tableGroupId);
            final var actual = tableGroupRepository.findById(tableGroupId);
            assertThat(actual).isEmpty();
        }

        @DisplayName("지정된 주문 테이블의 모든 계산이 완료되어 있어야 한다")
        @Test
        void ungroupWithUnreadyOrderTable() {
            final var tableGroupId = tableGroup.getId();
            saveOrder(tableGroupId);

            assertThatThrownBy(() -> tableGroupService.ungroup(tableGroupId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("계산이 완료되지 않은 테이블이 존재합니다.");
        }
    }

    private OrderTable makeEmptyOrderTable() {
        return new OrderTable(new GuestCount(0), true);
    }

    private OrderTable makeNonEmptyOrderTable(final int numberOfGuests) {
        return new OrderTable(new GuestCount(numberOfGuests), false);
    }

    private void saveOrder(final Long orderTableId) {
        final var order = new Order(orderTableId, makeSingleOrderLineItems());
        orderRepository.save(order);
    }

    private List<OrderLineItem> makeSingleOrderLineItems() {
        return List.of(new OrderLineItem(1L, 10));
    }
}
