package com.blind.orderflow.order.service;

import com.blind.orderflow.order.entity.Order;
import com.blind.orderflow.order.entity.OrderItem;
import com.blind.orderflow.order.repository.OrderItemRepository;
import com.blind.orderflow.order.repository.OrderRepository;
import com.blind.orderflow.order.state.OrderStateMachine;
import com.blind.orderflow.shared.events.BaseEvent;
import com.blind.orderflow.shared.events.OrderCreatedPayload;
import com.blind.orderflow.shared.exceptions.OrderNotFoundException;
import com.blind.orderflow.shared.kafka.KafkaProducerService;
import com.blind.orderflow.shared.utils.enums.OrderStatus;
import com.blind.orderflow.shared.utils.logging.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.blind.orderflow.config.KafkaConfig.ORDER_CREATED_TOPIC;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final KafkaProducerService kafkaProducerService;

    public Mono<Order> createOrder(String customerId, Flux<OrderItem> items) {

        LocalDateTime start = LocalDateTime.now();
        String orderId = UUID.randomUUID().toString();

        Logger.info(
                orderId,
                "ORDER",
                "ENTRY_CREATE_ORDER",
                "START",
                "Creating new order"
        );

        Order order = Order.builder()
                .orderId(orderId)
                .customerId(customerId)
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        OrderStateMachine.validate(order.getStatus(), OrderStatus.PENDING_PAYMENT);
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        return orderRepository.save(order)
                .flatMap(savedOrder ->
                        items
                                .doOnNext(item -> item.setOrderId(orderId))
                                .flatMap(orderItemRepository::save)
                                .then(Mono.just(savedOrder))
                )
                .flatMap(savedOrder -> {

                    Logger.info(
                            orderId,
                            "ORDER",
                            "CREATE_ORDER",
                            Logger.processDuration(start),
                            "Order persisted successfully"
                    );

                    // Create event payload
                    OrderCreatedPayload payload =
                            OrderCreatedPayload.builder()
                                    .orderId(orderId)
                                    .customerId(customerId)
                                    .totalAmount(savedOrder.getTotalAmount())
                                    .build();

                    BaseEvent<OrderCreatedPayload> event =
                            BaseEvent.<OrderCreatedPayload>builder()
                                    .eventId(UUID.randomUUID())
                                    .eventType("OrderCreated")
                                    .version(1)
                                    .occurredAt(Instant.now())
                                    .payload(payload)
                                    .build();

                    return kafkaProducerService
                            .send(ORDER_CREATED_TOPIC, orderId, event)
                            .doOnSuccess(v ->
                                    Logger.info(
                                            orderId,
                                            "KAFKA",
                                            "PUBLISH_EVENT",
                                            Logger.processDuration(start),
                                            "order.created event published"
                                    )
                            )
                            .thenReturn(savedOrder);
                }).doOnError(throwable -> {
                    Logger.error(
                            orderId,
                            "ORDER",
                            "ERROR_CREATE_ORDER",
                            Logger.processDuration(start),
                            "Error creating order: " + throwable.getMessage()
                    );
                });
    }

    public Mono<Order> getOrder(String orderId) {

        LocalDateTime start = LocalDateTime.now();

        Logger.info(
                orderId,
                "ORDER",
                "GET_ORDER",
                "START",
                "Fetching order"
        );

        return orderRepository
                .findByOrderId(orderId)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
                .doOnSuccess(order ->
                        Logger.info(
                                orderId,
                                "ORDER",
                                "GET_ORDER",
                                Logger.processDuration(start),
                                "Order retrieved successfully"
                        )
                ).doOnError(throwable -> {
                    Logger.error(
                            orderId,
                            "ORDER",
                            "ERROR_GET_ORDER",
                            Logger.processDuration(start),
                            "Error fetching order: " + throwable.getMessage()
                    );
                });
    }

    public Flux<OrderItem> getOrderItems(String orderId) {

        LocalDateTime start = LocalDateTime.now();

        Logger.info(
                orderId,
                "ORDER",
                "GET_ORDER_ITEMS",
                "START",
                "Fetching order items"
        );

        return orderItemRepository
                .findByOrderId(orderId)
                .doOnComplete(() ->
                        Logger.info(
                                orderId,
                                "ORDER",
                                "GET_ORDER_ITEMS",
                                Logger.processDuration(start),
                                "Order items retrieved"
                        )
                ).doOnError(throwable -> {
                    Logger.error(
                            orderId,
                            "ORDER",
                            "ERROR_GET_ORDER_ITEMS",
                            Logger.processDuration(start),
                            "Error fetching order items: " + throwable.getMessage()
                    );
                });
    }
}