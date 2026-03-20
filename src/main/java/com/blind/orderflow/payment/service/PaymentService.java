package com.blind.orderflow.payment.service;

import com.blind.orderflow.config.KafkaConfig;
import com.blind.orderflow.order.repository.OrderRepository;
import com.blind.orderflow.payment.entity.PaymentTransaction;
import com.blind.orderflow.payment.repository.PaymentRepository;
import com.blind.orderflow.shared.events.BaseEvent;
import com.blind.orderflow.shared.events.PaymentCompletedPayload;
import com.blind.orderflow.shared.events.PaymentFailedPayload;
import com.blind.orderflow.shared.kafka.KafkaProducerService;
import com.blind.orderflow.shared.utils.enums.OrderStatus;
import com.blind.orderflow.shared.utils.logging.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaProducerService kafkaProducerService;
    private final OrderRepository orderRepository;

    public Mono<Void> processPayment(String orderId) {

        LocalDateTime start = LocalDateTime.now();

        Mono<Void> pipeline=  paymentRepository
                .findByOrderId(orderId)

                //  skip dups
                .flatMap(existing -> {
                    Logger.info(
                            orderId,
                            "PAYMENT",
                            "SKIP_DUPLICATE",
                            "INFO",
                            "Payment already processed"
                    );
                    return Mono.<Void>empty();
                })
                .switchIfEmpty(
                        orderRepository.findByOrderId(orderId)
                                .flatMap(order -> {

                                    if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                                        Logger.info(orderId, "PAYMENT", "SKIP_INVALID_STATE", "INFO", "Invalid state");
                                        return Mono.empty();
                                    }

                                    return processNewPayment(orderId);
                                })
                );

        return Logger.logMono(pipeline, "PAYMENT", "PROCESS_PAYMENT", start);
    }


    private Mono<Void> processNewPayment(String orderId) {

        LocalDateTime start = LocalDateTime.now();
        String transactionId = UUID.randomUUID().toString();

        Mono<Void> pipeline= orderRepository.findByOrderId(orderId)
                .flatMap(order -> {

                    if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                        Logger.info(
                                orderId,
                                "PAYMENT",
                                "SKIP_INVALID_STATE",
                                "INFO",
                                "Order not in PENDING_PAYMENT"
                        );
                        return Mono.empty();
                    }

                    PaymentTransaction tx =
                            PaymentTransaction.builder()
                                    .transactionId(transactionId)
                                    .orderId(orderId)
                                    .amount(order.getTotalAmount())
                                    .status("PROCESSING")
                                    .createdAt(LocalDateTime.now())
                                    .build();

                    return paymentRepository.save(tx);
                })
                .flatMap(saved -> {

                    boolean success = new Random().nextBoolean();

                    String decision = success ? "SUCCESS" : "FAILED";


                    Logger.info(
                            orderId,
                            "PAYMENT",
                            "PAYMENT_DECISION",
                            "INFO",
                            "Payment decision = " + decision
                    );



                    if (success) {
                        saved.setStatus("SUCCESS");
                        saved.setUpdatedAt(LocalDateTime.now());

                        Logger.info(
                                orderId,
                                "PAYMENT",
                                "PAYMENT_SUCCESS",
                                "INFO",
                                "Payment approved, publishing PAYMENT_COMPLETED event"
                        );

                        return paymentRepository.save(saved)
                                .then(kafkaProducerService.send(
                                        KafkaConfig.PAYMENT_COMPLETED_TOPIC,
                                        orderId,
                                        buildCompletedEvent(orderId, transactionId)
                                ));
                    } else {

                        Logger.info(
                                orderId,
                                "PAYMENT",
                                "PAYMENT_SUCCESS",
                                "INFO",
                                "Payment approved, publishing PAYMENT_COMPLETED event"
                        );

                        saved.setStatus("FAILED");
                        saved.setUpdatedAt(LocalDateTime.now());

                        return paymentRepository.save(saved)
                                .then(kafkaProducerService.send(
                                        KafkaConfig.PAYMENT_FAILED_TOPIC,
                                        orderId,
                                        buildFailedEvent(orderId)
                                ));
                    }
                })
                .doOnSuccess(v ->
                        Logger.info(orderId, "PAYMENT", "EVENT_PUBLISHED", "SUCCESS",
                                "Payment event published to Kafka")
                )
                .doOnError(e ->
                        Logger.error(orderId, "PAYMENT", "EVENT_PUBLISH_FAILED", "ERROR",
                                e.getMessage())
                );

        return Logger.logMono(pipeline, "PAYMENT", "PROCESS_NEW_PAYMENT", start);
    }

    private BaseEvent<PaymentCompletedPayload> buildCompletedEvent(String orderId, String transactionId) {

        PaymentCompletedPayload payload =
                PaymentCompletedPayload.builder()
                        .orderId(orderId)
                        .transactionId(transactionId)
                        .build();

        return BaseEvent.<PaymentCompletedPayload>builder()
                .eventId(UUID.randomUUID())
                .eventType("PaymentCompleted")
                .version(1)
                .occurredAt(Instant.now())
                .payload(payload)
                .build();
    }

    private BaseEvent<PaymentFailedPayload> buildFailedEvent(String orderId) {

        PaymentFailedPayload payload =
                PaymentFailedPayload.builder()
                        .orderId(orderId)
                        .reason("Payment declined")
                        .build();

        return BaseEvent.<PaymentFailedPayload>builder()
                .eventId(UUID.randomUUID())
                .eventType("PaymentFailed")
                .version(1)
                .occurredAt(Instant.now())
                .payload(payload)
                .build();
    }
}