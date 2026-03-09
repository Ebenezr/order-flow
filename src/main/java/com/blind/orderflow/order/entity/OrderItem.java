package com.blind.orderflow.order.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("order_items")
public class OrderItem {

    @Id
    private Long id;

    private String orderId;

    private String productId;

    private String productName;

    private Integer quantity;

    private Double price;

    // snapshot JSON
    private String productSnapshot;
}