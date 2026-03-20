package com.blind.orderflow.inventory.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("inventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    @Id
    private Long id;

    private String ingredientId;
    private String ingredientName;
    private String category;
    private Integer availableQuantity;
    private LocalDateTime updatedAt;
}