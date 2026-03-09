package com.blind.orderflow.order.entity;


import com.blind.orderflow.shared.utils.enums.OrderStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("orders")
public class Order {

    @Id
    private Long id;

    private String orderId;

    private String customerId;

    private OrderStatus status;

    private Double totalAmount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}