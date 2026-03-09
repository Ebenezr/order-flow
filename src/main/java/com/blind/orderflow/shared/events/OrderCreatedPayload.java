package com.blind.orderflow.shared.events;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderCreatedPayload {

    private String orderId;
    private String customerId;
    private Double totalAmount;
}