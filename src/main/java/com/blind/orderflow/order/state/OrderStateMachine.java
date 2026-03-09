package com.blind.orderflow.order.state;

import com.blind.orderflow.shared.utils.enums.OrderStatus;
import com.blind.orderflow.shared.exceptions.OrderStateTransitionException;

import java.util.Map;
import java.util.Set;

public class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> transitions = Map.of(
            OrderStatus.CREATED, Set.of(OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED),
            OrderStatus.PENDING_PAYMENT, Set.of(OrderStatus.CONFIRMED, OrderStatus.PAYMENT_FAILED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.IN_KITCHEN),
            OrderStatus.IN_KITCHEN, Set.of(OrderStatus.READY),
            OrderStatus.READY, Set.of(OrderStatus.COMPLETED)
    );

    public static void validate(OrderStatus current, OrderStatus next) {

        Set<OrderStatus> allowed = transitions.get(current);

        if (allowed == null || !allowed.contains(next)) {
            throw new OrderStateTransitionException(current.name(), next.name());
        }
    }
}