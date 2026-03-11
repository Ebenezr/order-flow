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

        Order order = Order.builder()
                .orderId(orderId)
                .customerId(customerId)
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        OrderStateMachine.validate(order.getStatus(), OrderStatus.PENDING_PAYMENT);
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        Mono<Order> pipeline =
                orderRepository.save(order)
                        .flatMap(savedOrder ->
                                items
                                        .doOnNext(item -> item.setOrderId(orderId))
                                        .flatMap(orderItemRepository::save)
                                        .collectList()
                                        .flatMap(savedItems -> {
                                            double subtotal = savedItems.stream()
                                                    .mapToDouble(item -> item.getPrice() * item.getQuantity())
                                                    .sum();
                                            double vat = subtotal * 0.15;

                                            double serviceCharge = 0;

                                            double discount = 0;

                                            double total = subtotal + vat + serviceCharge - discount;

                                            savedOrder.setTotalAmount(total);
                                            savedOrder.setServiceCharge(serviceCharge);
                                            savedOrder.setDiscount(discount);
                                            savedOrder.setVat(vat);
                                            savedOrder.setSubtotal(subtotal);
                                            return orderRepository.save(savedOrder);
                                        })
                        )
                        .flatMap(savedOrder -> {



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
                                    .thenReturn(savedOrder);
                        });

        return Logger.logMono(pipeline, "ORDER", "CREATE_ORDER", start);
    }


    public Mono<Order> getOrder(String orderId) {

        LocalDateTime start = LocalDateTime.now();

        Mono<Order> pipeline =
                orderRepository
                        .findByOrderId(orderId)
                        .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)));

        return Logger.logMono(
                pipeline,
                "ORDER",
                "GET_ORDER",
                start
        );
    }
    public Flux<OrderItem> getOrderItems(String orderId) {

        LocalDateTime start = LocalDateTime.now();

        Flux<OrderItem> pipeline =
                orderItemRepository.findByOrderId(orderId);

        return Logger.logFlux(
                pipeline,
                "ORDER",
                "GET_ORDER_ITEMS",
                start
        );
    }

    public Mono<Order> cancelOrder(String orderId) {
        return cancelOrder(orderId, "manual");
    }

    public Mono<Order> cancelOrder(String orderId, String reason) {

        LocalDateTime start = LocalDateTime.now();

        Logger.info(
                orderId,
                "ORDER",
                "CANCEL_ORDER",
                "START",
                "Cancelling order"
        );

        Mono<Order> pipeline =
                orderRepository
                        .findByOrderId(orderId)
                        .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
                        .flatMap(order -> {
                            OrderStateMachine.validate(order.getStatus(), OrderStatus.CANCELLED);
                            order.setStatus(OrderStatus.CANCELLED);
                            order.setCancellationReason(reason);
                            order.setUpdatedAt(LocalDateTime.now());
                            return orderRepository.save(order);
                        });

        return Logger.logMono(
                pipeline,
                "ORDER",
                "CANCEL_ORDER",
                start
        );
    }

    public Mono<Order> markPaymentConfirmed(String orderId) {

        LocalDateTime start = LocalDateTime.now();

        Logger.info(
                orderId,
                "ORDER",
                "PAYMENT_CONFIRMED",
                "START",
                "Marking order as payment confirmed"
        );

        Mono<Order> pipeline =
                orderRepository
                        .findByOrderId(orderId)
                        .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
                        .flatMap(order -> {

                            OrderStateMachine.validate(order.getStatus(), OrderStatus.CONFIRMED);

                            order.setStatus(OrderStatus.CONFIRMED);
                            order.setUpdatedAt(LocalDateTime.now());

                            return orderRepository.save(order);
                        });

        return Logger.logMono(
                pipeline,
                "ORDER",
                "PAYMENT_CONFIRMED",
                start
        );
    }

    public Mono<Order> confirmOrder(String orderId) {

        LocalDateTime start = LocalDateTime.now();

        Mono<Order> pipeline =
                orderRepository
                        .findByOrderId(orderId)
                        .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
                        .flatMap(order -> {

                            order.setStatus(OrderStatus.CONFIRMED);
                            order.setUpdatedAt(LocalDateTime.now());

                            return orderRepository.save(order);
                        });

        return Logger.logMono(
                pipeline,
                "ORDER",
                "CONFIRM_ORDER",
                start
        );
    }

    public Mono<Order> updateOrderStatus(String orderId, OrderStatus status) {

        Logger.info(
                orderId,
                "ORDER",
                "UPDATE_ORDER_STATUS",
                "START",
                "Updating order status to " + status
        );

        return orderRepository.findByOrderId(orderId)

                .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))

                .flatMap(order -> {

                    order.setStatus(status);
                    order.setUpdatedAt(LocalDateTime.now());

                    return orderRepository.save(order);
                });
    }
}