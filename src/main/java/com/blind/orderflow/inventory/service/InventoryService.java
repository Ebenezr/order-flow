package com.blind.orderflow.inventory.service;

import com.blind.orderflow.config.KafkaConfig;
import com.blind.orderflow.inventory.entity.InventoryReservation;
import com.blind.orderflow.inventory.repository.InventoryRepository;
import com.blind.orderflow.inventory.repository.InventoryReservationRepository;
import com.blind.orderflow.menu.service.MenuService;
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
    private final MenuService menuService;

    public Mono<Void> reserveStock(String orderId,String correlationId) {

        LocalDateTime start = LocalDateTime.now();

        Mono<Void> pipeline= orderItemRepository.findByOrderId(orderId)
                .collectList()
                .flatMap(items -> {

                            if (items.isEmpty()) {
                                return Mono.error(new RuntimeException("No items in order"));
                            }

                           return Flux.fromIterable(items)
                                   .concatMap(item ->
                                           menuService.getItem(item.getProductId())
                                                   .flatMapMany(menuItem ->
                                                           Flux.fromIterable(menuItem.getRecipe())
                                                                   .concatMap(recipeItem -> {

                                                                       int requiredQty =
                                                                               recipeItem.getQuantity() * item.getQuantity();

                                                                       Logger.info(correlationId, "INVENTORY", "CHECK_INGREDIENT", "INFO",
                                                                               "Checking " + recipeItem.getIngredientId() + " qty=" + requiredQty);

                                                                       return inventoryRepository.reserveStock(
                                                                                       recipeItem.getIngredientId(),
                                                                                       requiredQty
                                                                               )
                                                                               .flatMap(rows -> {
                                                                                   if (rows == 0) {
                                                                                       return Mono.error(new RuntimeException(
                                                                                               "Out of stock: " + recipeItem.getIngredientId()
                                                                                       ));
                                                                                   }

                                                                                   return reservationRepository.save(
                                                                                           InventoryReservation.builder()
                                                                                                   .reservationId(UUID.randomUUID().toString())
                                                                                                   .orderId(orderId)
                                                                                                   .ingredientId(recipeItem.getIngredientId())
                                                                                                   .quantity(requiredQty)
                                                                                                   .status("RESERVED")
                                                                                                   .createdAt(LocalDateTime.now())
                                                                                                   .expiresAt(LocalDateTime.now().plusMinutes(5))
                                                                                                   .build()
                                                                                   );
                                                                               });
                                                                   })
                                                   )
                                   )
                                    .then(Mono.just(true));
                        }
                )
                .flatMap(success -> {

                    Logger.info(correlationId, "INVENTORY", "SUCCESS_RESERVE", "SUCCESS", "All items reserved");


                    BaseEvent<InventoryReservedPayload> event =
                            BaseEvent.<InventoryReservedPayload>builder()
                                    .eventId(UUID.randomUUID())
                                    .correlationId(correlationId)
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
                })
                .onErrorResume(error -> {

                    Logger.error(correlationId, "INVENTORY", "RESERVE_FAILED", "ERROR", error.getMessage());

                    return rollbackReservations(orderId)
                            .then(publishInventoryFailed(orderId, error.getMessage(),correlationId));
                });
        return Logger.logMono(pipeline, "INVENTORY", "RESERVE_STOCK", start);
    }

    public Mono<Void> confirmReservation(String orderId, String correlationId) {
        LocalDateTime start = LocalDateTime.now();

        // convert RESERVED → CONFIRMED
        Mono<Void> pipeline= reservationRepository.findByOrderId(orderId)

                .flatMap(res -> {

                    res.setStatus("CONFIRMED");

                    return reservationRepository.save(res);
                })
                .doOnError(error ->
                        Logger.error(correlationId, "INVENTORY", "CONFIRM", "ERROR", error.getMessage())
                )
                .then();

        return Logger.logMono(pipeline, "INVENTORY", "CONFIRM_RESERVATION", start);
    }

    public Mono<Void> releaseReservation(String orderId) {
        LocalDateTime start = LocalDateTime.now();

        Mono<Void> pipeline= reservationRepository.findByOrderId(orderId)
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

        return Logger.logMono(pipeline, "INVENTORY", "RELEASE_STOCK", start);
    }

    private Mono<Void> publishInventoryFailed(String orderId, String reason,String correlationId) {

        LocalDateTime start = LocalDateTime.now();


        BaseEvent<InventoryFailedPayload> event =
                BaseEvent.<InventoryFailedPayload>builder()
                        .eventId(UUID.randomUUID())
                        .correlationId(correlationId)
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

        Mono<Void> pipeline= kafkaProducerService.send(
                KafkaConfig.INVENTORY_FAILED_TOPIC,
                orderId,
                event
        );

        return Logger.logMono(pipeline, "INVENTORY", "PUBLISH_INVENTORY_FAILED", start);
    }


    private Mono<Void> rollbackReservations(String orderId) {

        LocalDateTime start = LocalDateTime.now();

        Mono<Void> pipeline= reservationRepository.findByOrderId(orderId)
                .flatMap(res ->
                        inventoryRepository.releaseStock(
                                        res.getIngredientId(),
                                        res.getQuantity()
                                ).then(reservationRepository.delete(res)))
                .then();

        return Logger.logMono(pipeline, "INVENTORY", "ROLLBACK_RESERVATION", start);
    }

}
