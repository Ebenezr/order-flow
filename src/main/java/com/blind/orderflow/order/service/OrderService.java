package com.blind.orderflow.order.service;

import com.blind.orderflow.menu.service.MenuService;
import com.blind.orderflow.order.dto.CreateOrderItemRequest;
import com.blind.orderflow.order.entity.Order;
import com.blind.orderflow.order.entity.OrderItem;
import com.blind.orderflow.order.repository.OrderItemRepository;
import com.blind.orderflow.order.repository.OrderRepository;
import com.blind.orderflow.order.state.OrderStateMachine;
import com.blind.orderflow.shared.events.BaseEvent;
import com.blind.orderflow.shared.events.OrderCreatedPayload;
import com.blind.orderflow.shared.events.OrderItemPayload;
import com.blind.orderflow.shared.exceptions.OrderNotFoundException;
import com.blind.orderflow.shared.kafka.KafkaProducerService;
import com.blind.orderflow.shared.utils.enums.OrderStatus;
import com.blind.orderflow.shared.utils.logging.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.blind.orderflow.config.KafkaConfig.ORDER_CREATED_TOPIC;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final KafkaProducerService kafkaProducerService;
    private final MenuService menuService;

    public Mono<Order> createOrder(String customerId, Flux<CreateOrderItemRequest> items) {

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
                                        .flatMap(req ->
                                                menuService.getItem(req.getProductId())
                                                        .map(menuItem -> {

                                                            OrderItem item = new OrderItem();
                                                            item.setOrderId(orderId);
                                                            item.setProductId(menuItem.getProductId());
                                                            item.setProductName(menuItem.getName());
                                                            item.setPrice(BigDecimal.valueOf(menuItem.getPrice()));
                                                            item.setQuantity(req.getQuantity());
                                                            item.setProductSnapshot(menuItem.toString());

                                                            return item;
                                                        })
                                        )
                                        .flatMap(orderItemRepository::save)
                                        .collectList()
                                        .map(savedItems -> Tuples.of(savedOrder, savedItems))
                        )
                        .flatMap(tuple -> {


                            Order savedOrder = tuple.getT1();
                            List<OrderItem> savedItems = tuple.getT2();

                            BigDecimal subtotal = savedItems.stream()
                                    .map(item ->
                                            item.getPrice()
                                                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                                    )
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                            BigDecimal vat = subtotal.multiply(BigDecimal.valueOf(0.15));

                            BigDecimal serviceCharge = BigDecimal.ZERO;
                            BigDecimal discount = BigDecimal.ZERO;

                            BigDecimal total = subtotal
                                    .add(vat)
                                    .add(serviceCharge)
                                    .subtract(discount);

                            savedOrder.setSubtotal(subtotal);
                            savedOrder.setVat(vat);
                            savedOrder.setServiceCharge(serviceCharge);
                            savedOrder.setDiscount(discount);
                            savedOrder.setTotalAmount(total);

                            return orderRepository.save(savedOrder)
                                    .map(updated -> Tuples.of(updated, savedItems));
                        })
                        .flatMap(tuple -> {

                            Order savedOrder = tuple.getT1();
                            List<OrderItem> savedItems = tuple.getT2();

                            List<OrderItemPayload> itemPayloads =
                                    savedItems.stream()
                                            .map(item ->
                                                    OrderItemPayload.builder()
                                                            .productId(item.getProductId())
                                                            .productName(item.getProductName())
                                                            .quantity(item.getQuantity())
                                                            .build()
                                            )
                                            .toList();

                            OrderCreatedPayload payload =
                                    OrderCreatedPayload.builder()
                                            .orderId(savedOrder.getOrderId())
                                            .customerId(savedOrder.getCustomerId())
                                            .totalAmount(savedOrder.getTotalAmount())
                                            .items(itemPayloads)
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
                                    .send(ORDER_CREATED_TOPIC, savedOrder.getOrderId(), event)
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

        return orderRepository.findByOrderId(orderId)
                .switchIfEmpty(Mono.error(new OrderNotFoundException(orderId)))
                .flatMap(order -> {

                    // ✅ already cancelled → just return (idempotent)
                    if (order.getStatus() == OrderStatus.CANCELLED) {
                        Logger.info(
                                orderId,
                                "ORDER",
                                "CANCEL_ORDER_SKIP",
                                "INFO",
                                "Order already cancelled"
                        );
                        return Mono.just(order);
                    }

                    // normal flow
                    OrderStateMachine.validate(order.getStatus(), OrderStatus.CANCELLED);

                    order.setStatus(OrderStatus.CANCELLED);
                    order.setUpdatedAt(LocalDateTime.now());

                    return orderRepository.save(order);
                });
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