package com.blind.orderflow.payment.service;

import com.blind.orderflow.config.KafkaConfig;
import com.blind.orderflow.order.repository.OrderRepository;
import com.blind.orderflow.payment.entity.PaymentTransaction;
import com.blind.orderflow.payment.repository.PaymentRepository;
import com.blind.orderflow.shared.events.BaseEvent;
import com.blind.orderflow.shared.events.PaymentCompletedPayload;
import com.blind.orderflow.shared.events.PaymentFailedPayload;
import com.blind.orderflow.shared.kafka.KafkaProducerService;
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
        Logger.info(
                orderId,
                "PAYMENT",
                "ENTRY_PROCESS_PAYMENT",
                "START",
                "Processing payment for order"
        );

        return paymentRepository
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
                    return Mono.empty();
                })

                // If no payment exists → process normally
                .switchIfEmpty(processNewPayment(orderId))

                .then();
    }


    private Mono<Void> processNewPayment(String orderId) {

        String transactionId = UUID.randomUUID().toString();

        Logger.info(orderId,"PAYMENT","PROCESS_PAYMENT","START","Processing payment");

        return orderRepository.findByOrderId(orderId)
                .flatMap(order -> {
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

                    if (success) {
                        saved.setStatus("SUCCESS");

                        return paymentRepository.save(saved)
                                .then(kafkaProducerService.send(
                                        KafkaConfig.PAYMENT_COMPLETED_TOPIC,
                                        orderId,
                                        buildCompletedEvent(orderId, transactionId)
                                ));
                    } else {
                        saved.setStatus("FAILED");

                        return paymentRepository.save(saved)
                                .then(kafkaProducerService.send(
                                        KafkaConfig.PAYMENT_FAILED_TOPIC,
                                        orderId,
                                        buildFailedEvent(orderId)
                                ));
                    }
                });
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