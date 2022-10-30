package kitchenpos.application.order.dto.response;

import kitchenpos.domain.order.Order;
import kitchenpos.domain.order.OrderLineItem;
import kitchenpos.domain.order.OrderTable;
import kitchenpos.domain.order.TableGroup;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderResponseAssembler {

    public List<OrderResponse> asOrderResponses(final List<Order> orders) {
        return orders.stream()
                .map(this::asOrderResponse)
                .collect(Collectors.toUnmodifiableList());
    }

    public OrderResponse asOrderResponse(final Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderTableId(),
                order.getOrderStatus().name(),
                order.getOrderedTime(),
                asOrderLineItemResponses(order.getOrderLineItems())
        );
    }

    private List<OrderLineItemResponse> asOrderLineItemResponses(final List<OrderLineItem> orderLineItems) {
        return orderLineItems.stream()
                .map(this::asOrderLineItemResponse)
                .collect(Collectors.toUnmodifiableList());
    }

    private OrderLineItemResponse asOrderLineItemResponse(final OrderLineItem orderLineItem) {
        return new OrderLineItemResponse(
                orderLineItem.getSeq(),
                orderLineItem.getMenuId(),
                orderLineItem.getQuantity()
        );
    }

    public TableGroupResponse asTableGroupResponse(final TableGroup tableGroup) {
        return new TableGroupResponse(
                tableGroup.getId(),
                tableGroup.getCreatedDate(),
                asOrderTableResponses(tableGroup.getOrderTables())
        );
    }

    public List<OrderTableResponse> asOrderTableResponses(final List<OrderTable> orderTables) {
        return orderTables.stream()
                .map(this::asOrderTableResponse)
                .collect(Collectors.toUnmodifiableList());
    }

    public OrderTableResponse asOrderTableResponse(final OrderTable orderTable) {
        return new OrderTableResponse(
                orderTable.getId(),
                orderTable.getNumberOfGuests().getValue(),
                orderTable.isEmpty()
        );
    }
}
