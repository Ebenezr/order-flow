package com.blind.orderflow.shared.exceptions;

public class OrderStateTransitionException extends BusinessException {

    public OrderStateTransitionException(String from, String to) {
        super("Invalid order state transition from " + from + " to " + to,
                "INVALID_ORDER_STATE");
    }
}