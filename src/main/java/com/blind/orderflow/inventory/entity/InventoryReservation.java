package com.blind.orderflow.inventory.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("inventory_reservations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservation {

    @Id
    private Long id;

    private String reservationId;
    private String orderId;
    private String ingredientId;
    private Integer quantity;
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;
}