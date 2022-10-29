package kitchenpos.application;

import kitchenpos.application.request.order.ChangeOrderStatusRequest;
import kitchenpos.application.request.order.OrderLineItemRequest;
import kitchenpos.application.request.order.OrderRequest;
import kitchenpos.application.response.ResponseAssembler;
import kitchenpos.application.response.order.OrderResponse;
import kitchenpos.dao.MenuDao;
import kitchenpos.dao.OrderDao;
import kitchenpos.dao.OrderLineItemDao;
import kitchenpos.dao.OrderTableDao;
import kitchenpos.domain.Order;
import kitchenpos.domain.OrderLineItem;
import kitchenpos.domain.OrderStatus;
import kitchenpos.domain.OrderTable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final MenuDao menuDao;
    private final OrderDao orderDao;
    private final OrderLineItemDao orderLineItemDao;
    private final OrderTableDao orderTableDao;
    private final ResponseAssembler responseAssembler;

    public OrderService(final MenuDao menuDao, final OrderDao orderDao, final OrderLineItemDao orderLineItemDao,
                        final OrderTableDao orderTableDao, final ResponseAssembler responseAssembler) {
        this.menuDao = menuDao;
        this.orderDao = orderDao;
        this.orderLineItemDao = orderLineItemDao;
        this.orderTableDao = orderTableDao;
        this.responseAssembler = responseAssembler;
    }

    @Transactional
    public OrderResponse create(final OrderRequest request) {
        final var orderLineItems = request.getOrderLineItems()
                .stream()
                .map(this::asOrderLineItem)
                .collect(Collectors.toUnmodifiableList());
        validateOrderLineItemsNotEmpty(orderLineItems);
        validateMenuNotDuplicated(orderLineItems);

        final var orderTableId = request.getOrderTableId();
        final OrderTable orderTable = orderTableDao.findById(orderTableId)
                .orElseThrow(() -> new IllegalArgumentException("주문 테이블을 찾을 수 없습니다."));
        validateOrderTableNotEmpty(orderTable);

        final var order = asOrder(request, orderLineItems);
        final Order savedOrder = orderDao.save(order);
        final Long orderId = savedOrder.getId();
        final List<OrderLineItem> savedOrderLineItems = new ArrayList<>();
        for (final OrderLineItem orderLineItem : orderLineItems) {
            orderLineItem.setOrderId(orderId);
            savedOrderLineItems.add(orderLineItemDao.save(orderLineItem));
        }
        savedOrder.setOrderLineItems(savedOrderLineItems);

        return responseAssembler.orderResponse(savedOrder);
    }

    private OrderLineItem asOrderLineItem(final OrderLineItemRequest request) {
        final var orderLineItem = new OrderLineItem();
        orderLineItem.setMenuId(request.getMenuId());
        orderLineItem.setQuantity(request.getQuantity());
        return orderLineItem;
    }

    private Order asOrder(final OrderRequest request, final List<OrderLineItem> orderLineItems) {
        final var order = new Order();
        order.setOrderTableId(request.getOrderTableId());
        order.setOrderStatus(OrderStatus.COOKING.name());
        order.setOrderedTime(LocalDateTime.now());
        order.setOrderLineItems(orderLineItems);
        return order;
    }

    private void validateOrderLineItemsNotEmpty(List<OrderLineItem> orderLineItems) {
        if (CollectionUtils.isEmpty(orderLineItems)) {
            throw new IllegalArgumentException("주문 항목이 비어 있습니다.");
        }
    }

    private void validateMenuNotDuplicated(List<OrderLineItem> orderLineItems) {
        final List<Long> menuIds = orderLineItems.stream()
                .map(OrderLineItem::getMenuId)
                .collect(Collectors.toList());

        if (orderLineItems.size() != menuDao.countByIdIn(menuIds)) {
            throw new IllegalArgumentException("중복된 메뉴의 주문 항목이 존재합니다.");
        }
    }

    private void validateOrderTableNotEmpty(OrderTable orderTable) {
        if (orderTable.isEmpty()) {
            throw new IllegalArgumentException("주문 테이블이 비어 있습니다.");
        }
    }

    public List<OrderResponse> list() {
        final List<Order> orders = orderDao.findAll();

        for (final Order order : orders) {
            order.setOrderLineItems(orderLineItemDao.findAllByOrderId(order.getId()));
        }

        return responseAssembler.orderResponses(orders);
    }

    @Transactional
    public OrderResponse changeOrderStatus(final Long orderId, final ChangeOrderStatusRequest order) {
        final Order savedOrder = orderDao.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문이 존재하지 않습니다."));
        validateOrderNotYestCompleted(savedOrder);

        final OrderStatus orderStatus = OrderStatus.valueOf(order.getOrderStatus());
        savedOrder.setOrderStatus(orderStatus.name());

        orderDao.save(savedOrder);
        savedOrder.setOrderLineItems(orderLineItemDao.findAllByOrderId(orderId));

        return responseAssembler.orderResponse(savedOrder);
    }

    private void validateOrderNotYestCompleted(Order savedOrder) {
        if (Objects.equals(OrderStatus.COMPLETION.name(), savedOrder.getOrderStatus())) {
            throw new IllegalArgumentException("이미 결제 완료된 주문입니다.");
        }
    }
}
