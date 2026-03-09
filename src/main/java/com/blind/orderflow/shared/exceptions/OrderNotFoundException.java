package com.blind.orderflow.shared.exceptions;

public class OrderNotFoundException extends NotFoundException {

    public OrderNotFoundException(String orderId) {
        super("Order not found: " + orderId);
    }
}