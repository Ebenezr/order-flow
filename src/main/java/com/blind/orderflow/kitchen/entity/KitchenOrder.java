package com.blind.orderflow.kitchen.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("kitchen_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KitchenOrder {

    @Id
    private Long id;

    private String kitchenOrderId;

    private String orderId;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}