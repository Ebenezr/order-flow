package com.blind.orderflow.order.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

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

    private BigDecimal price;

    private String category;

    // snapshot JSON
    private String productSnapshot;
}