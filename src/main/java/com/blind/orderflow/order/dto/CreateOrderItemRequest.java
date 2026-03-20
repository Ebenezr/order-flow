package com.blind.orderflow.order.dto;

import lombok.Data;

@Data
public class CreateOrderItemRequest {
    private String productId;
    private int quantity;
}