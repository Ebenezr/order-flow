package com.blind.orderflow.inventory.service;

import com.blind.orderflow.config.KafkaConfig;
import com.blind.orderflow.inventory.entity.InventoryReservation;
import com.blind.orderflow.inventory.repository.InventoryRepository;
import com.blind.orderflow.inventory.repository.InventoryReservationRepository;
import com.blind.orderflow.order.repository.OrderItemRepository;
import com.blind.orderflow.shared.events.BaseEvent;
import com.blind.orderflow.shared.events.InventoryFailedPayload;
import com.blind.orderflow.shared.events.InventoryReservedPayload;
import com.blind.orderflow.shared.kafka.KafkaProducerService;
import com.blind.orderflow.shared.utils.logging.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final KafkaProducerService kafkaProducerService;
    private final OrderItemRepository orderItemRepository;

    public Mono<Void> reserveStock(String orderId) {

        Logger.info(
                orderId,
                "INVENTORY",
                "ENTRY_RESERVE_STOCK",
                "START",
                "Reserving stock for order"
        );

        return orderItemRepository.findByOrderId(orderId)
                .collectList()
                .flatMap(items -> {
                            if (items.isEmpty()) {
                                return Mono.error(new RuntimeException("No items in order"));
                            }
                           return Flux.fromIterable(items)
                                    .concatMap(item ->

                                            inventoryRepository.reserveStock(
                                                            item.getProductId(),
                                                            item.getQuantity()
                                                    )
                                                    .flatMap(rows -> {
                                                        if (rows == 0) {
                                                            return Mono.error(new RuntimeException(
                                                                    "Out of stock: " + item.getProductId()
                                                            ));
                                                        }

                                                        InventoryReservation reservation =
                                                                InventoryReservation.builder()
                                                                        .reservationId(UUID.randomUUID().toString())
                                                                        .orderId(orderId)
                                                                        .ingredientId(item.getProductId())
                                                                        .quantity(item.getQuantity())
                                                                        .status("RESERVED")
                                                                        .createdAt(LocalDateTime.now())
                                                                        .expiresAt(LocalDateTime.now().plusMinutes(5))
                                                                        .build();

                                                        return reservationRepository.save(reservation);
                                                    })
                                    )
                                    .then();
                        }
                )
                .then(Mono.defer(() -> {

                    Logger.info(orderId, "INVENTORY", "SUCCESS_RESERVE", "SUCCESS", "All items reserved");


                    BaseEvent<InventoryReservedPayload> event =
                            BaseEvent.<InventoryReservedPayload>builder()
                                    .eventId(UUID.randomUUID())
                                    .eventType("InventoryReserved")
                                    .version(1)
                                    .occurredAt(Instant.now())
                                    .payload(
                                            InventoryReservedPayload.builder()
                                                    .orderId(orderId)
                                                    .build()
                                    )
                                    .build();

                    return kafkaProducerService.send(
                            KafkaConfig.INVENTORY_RESERVED_TOPIC,
                            orderId,
                            event
                    );
                }))
                .onErrorResume(error -> {

                    Logger.error(orderId, "INVENTORY", "RESERVE_FAILED", "ERROR", error.getMessage());

                    return rollbackReservations(orderId)
                            .then(publishInventoryFailed(orderId, error.getMessage()));
                });
    }

    public Mono<Void> confirmReservation(String orderId) {

        Logger.info(
                orderId,
                "INVENTORY",
                "CONFIRM_RESERVATION",
                "START",
                "Confirming inventory reservation for order"
        );

        // convert RESERVED → CONFIRMED
        return reservationRepository.findByOrderId(orderId)

                .flatMap(res -> {

                    res.setStatus("CONFIRMED");

                    return reservationRepository.save(res);
                })
                .doOnError(error ->
                        Logger.error(orderId, "INVENTORY", "CONFIRM", "ERROR", error.getMessage())
                )
                .then();
    }

    public Mono<Void> releaseReservation(String orderId) {
        Logger.info(
                orderId,
                "INVENTORY",
                "RELEASE_RESERVATION",
                "START",
                "Releasing inventory reservation for order"
        );

        return reservationRepository.findByOrderId(orderId)
                .flatMap(res ->

                        inventoryRepository.releaseStock(
                                        res.getIngredientId(),
                                        res.getQuantity()
                                )
                                .then(
                                        Mono.fromRunnable(() -> res.setStatus("RELEASED"))
                                )
                                .then(reservationRepository.save(res))
                )
                .then();
    }

    private Mono<Void> publishInventoryFailed(String orderId, String reason) {

        Logger.info(
                orderId,
                "INVENTORY",
                "PUBLISH_INVENTORY_FAILED",
                "START",
                "Publishing inventory failed event for order"
        );

        BaseEvent<InventoryFailedPayload> event =
                BaseEvent.<InventoryFailedPayload>builder()
                        .eventId(UUID.randomUUID())
                        .eventType("InventoryFailed")
                        .version(1)
                        .occurredAt(Instant.now())
                        .payload(
                                InventoryFailedPayload.builder()
                                        .orderId(orderId)
                                        .reason(reason)
                                        .build()
                        )
                        .build();

        return kafkaProducerService.send(
                KafkaConfig.INVENTORY_FAILED_TOPIC,
                orderId,
                event
        );
    }


    private Mono<Void> rollbackReservations(String orderId) {

        return reservationRepository.findByOrderId(orderId)
                .flatMap(res ->
                        inventoryRepository.releaseStock(
                                        res.getIngredientId(),
                                        res.getQuantity()
                                ).then(reservationRepository.delete(res)))
                .then();
    }

}
