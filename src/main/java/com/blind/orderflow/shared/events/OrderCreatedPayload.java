package com.blind.orderflow.shared.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCreatedPayload {
    private String orderId;
    private String customerId;
    private BigDecimal totalAmount;

    private List<OrderItemPayload> items;

}