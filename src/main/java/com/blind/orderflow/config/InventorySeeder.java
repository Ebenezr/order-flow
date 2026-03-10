package com.blind.orderflow.config;

import com.blind.orderflow.inventory.entity.Inventory;
import com.blind.orderflow.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InventorySeeder {

    private final InventoryRepository inventoryRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {

        inventoryRepository.saveAll(List.of(

                Inventory.builder()
                        .productId("burger-001")
                        .productName("Cheese Burger")
                        .availableQuantity(50)
                        .updatedAt(LocalDateTime.now())
                        .build(),

                Inventory.builder()
                        .productId("soda-001")
                        .productName("Coca Cola")
                        .availableQuantity(100)
                        .updatedAt(LocalDateTime.now())
                        .build()

        )).subscribe();
    }
}