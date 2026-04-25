package com.blind.orderflow.payment.service;

import com.blind.orderflow.config.KafkaConfig;
import com.blind.orderflow.order.repository.OrderRepository;
import com.blind.orderflow.payment.dto.PaymentRequest;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaProducerService kafkaProducerService;
    private final OrderRepository orderRepository;

    /**
     * Client-driven payment: the frontend supplies method, details, and simulateSuccess flag.
     */
    public Mono<PaymentTransaction> processPayment(String orderId, PaymentRequest request, String correlationId) {

        LocalDateTime start = LocalDateTime.now();

        Mono<PaymentTransaction> pipeline = paymentRepository
                .findByOrderId(orderId)
                .flatMap(existing -> {
                    Logger.info(correlationId, "PAYMENT", "SKIP_DUPLICATE", "INFO",
                            "Payment already processed for order " + orderId);
                    return Mono.<PaymentTransaction>error(new IllegalStateException(
                            "Payment already processed for order " + orderId + " (status=" + existing.getStatus() + ")"));
                })
                .switchIfEmpty(
                        orderRepository.findByOrderId(orderId)
                                .switchIfEmpty(Mono.error(new IllegalArgumentException("Order not found: " + orderId)))
                                .flatMap(order -> {
                                    if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                                        Logger.info(correlationId, "PAYMENT", "SKIP_INVALID_STATE", "INFO",
                                                "Order not in PENDING_PAYMENT state");
                                        return Mono.error(new IllegalStateException(
                                                "Order is in " + order.getStatus() + " state, expected PENDING_PAYMENT"));
                                    }
                                    return processNewPayment(orderId, request, correlationId);
                                })
                );

        return Logger.logMono(pipeline, "PAYMENT", "PROCESS_PAYMENT", start);
    }


    // ──────────────────────── private helpers ────────────────────────

    private Mono<PaymentTransaction> processNewPayment(String orderId, PaymentRequest request, String correlationId) {

        LocalDateTime start = LocalDateTime.now();
        String transactionId = UUID.randomUUID().toString();

        validatePaymentInput(request);

        Mono<PaymentTransaction> pipeline = orderRepository.findByOrderId(orderId)
                .flatMap(order -> {

                    if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                        Logger.info(correlationId, "PAYMENT", "SKIP_INVALID_STATE", "INFO",
                                "Order not in PENDING_PAYMENT");
                        return Mono.<PaymentTransaction>empty();
                    }

                    PaymentTransaction tx = PaymentTransaction.builder()
                            .transactionId(transactionId)
                            .orderId(orderId)
                            .paymentMethod(request.getMethod())
                            .maskedDetail(maskDetail(request))
                            .amount(order.getTotalAmount())
                            .status("PROCESSING")
                            .createdAt(LocalDateTime.now())
                            .build();

                    return paymentRepository.save(tx);
                })
                .flatMap(saved -> {

                    boolean success = Boolean.TRUE.equals(request.getSimulateSuccess());
                    String decision = success ? "SUCCESS" : "FAILED";

                    Logger.info(correlationId, "PAYMENT", "PAYMENT_DECISION", "INFO",
                            "Payment decision = " + decision + " (method=" + request.getMethod() + ")");

                    if (success) {
                        saved.setStatus("SUCCESS");
                        saved.setUpdatedAt(LocalDateTime.now());

                        Logger.info(correlationId, "PAYMENT", "PAYMENT_SUCCESS", "INFO",
                                "Payment approved, publishing PAYMENT_COMPLETED event");

                        return paymentRepository.save(saved)
                                .flatMap(s -> kafkaProducerService.send(
                                        KafkaConfig.PAYMENT_COMPLETED_TOPIC,
                                        orderId,
                                        buildCompletedEvent(orderId, transactionId, correlationId)
                                ).thenReturn(s));
                    } else {

                        saved.setStatus("FAILED");
                        saved.setUpdatedAt(LocalDateTime.now());

                        Logger.info(correlationId, "PAYMENT", "PAYMENT_FAILED", "INFO",
                                "Payment declined, publishing PAYMENT_FAILED event");

                        return paymentRepository.save(saved)
                                .flatMap(s -> kafkaProducerService.send(
                                        KafkaConfig.PAYMENT_FAILED_TOPIC,
                                        orderId,
                                        buildFailedEvent(orderId, correlationId)
                                ).thenReturn(s));
                    }
                })
                .doOnSuccess(v ->
                        Logger.info(correlationId, "PAYMENT", "EVENT_PUBLISHED", "SUCCESS",
                                "Payment event published to Kafka"))
                .doOnError(e ->
                        Logger.error(correlationId, "PAYMENT", "EVENT_PUBLISH_FAILED", "ERROR",
                                e.getMessage()));

        return Logger.logMono(pipeline, "PAYMENT", "PROCESS_NEW_PAYMENT", start);
    }

    private void validatePaymentInput(PaymentRequest request) {
        String method = request.getMethod();
        if (!"MPESA".equalsIgnoreCase(method) && !"CARD".equalsIgnoreCase(method)) {
            throw new IllegalArgumentException("Unsupported payment method: " + method + ". Use MPESA or CARD.");
        }
        if ("MPESA".equalsIgnoreCase(method)) {
            if (request.getPhone() == null || request.getPhone().isBlank()) {
                throw new IllegalArgumentException("Phone number is required for MPESA payments");
            }
        }
        if ("CARD".equalsIgnoreCase(method)) {
            if (request.getCardNumber() == null || request.getCardNumber().isBlank()) {
                throw new IllegalArgumentException("Card number is required for CARD payments");
            }
            if (request.getExpiry() == null || request.getExpiry().isBlank()) {
                throw new IllegalArgumentException("Expiry is required for CARD payments");
            }
            if (request.getCvv() == null || request.getCvv().isBlank()) {
                throw new IllegalArgumentException("CVV is required for CARD payments");
            }
        }
    }

    /**
     * Masks sensitive details for storage – shows only last 4 chars.
     */
    private String maskDetail(PaymentRequest request) {
        if ("MPESA".equalsIgnoreCase(request.getMethod()) && request.getPhone() != null) {
            String phone = request.getPhone();
            return "****" + phone.substring(Math.max(0, phone.length() - 4));
        }
        if ("CARD".equalsIgnoreCase(request.getMethod()) && request.getCardNumber() != null) {
            String card = request.getCardNumber();
            return "****" + card.substring(Math.max(0, card.length() - 4));
        }
        return "****";
    }

    // ──────────────────────── event builders ────────────────────────

    private BaseEvent<PaymentCompletedPayload> buildCompletedEvent(String orderId, String transactionId, String correlationId) {
        PaymentCompletedPayload payload = PaymentCompletedPayload.builder()
                .orderId(orderId)
                .transactionId(transactionId)
                .build();

        return BaseEvent.<PaymentCompletedPayload>builder()
                .eventId(UUID.randomUUID())
                .correlationId(correlationId)
                .eventType("PaymentCompleted")
                .version(1)
                .occurredAt(Instant.now())
                .payload(payload)
                .build();
    }

    private BaseEvent<PaymentFailedPayload> buildFailedEvent(String orderId, String correlationId) {
        PaymentFailedPayload payload = PaymentFailedPayload.builder()
                .orderId(orderId)
                .reason("Payment declined")
                .build();

        return BaseEvent.<PaymentFailedPayload>builder()
                .eventId(UUID.randomUUID())
                .correlationId(correlationId)
                .eventType("PaymentFailed")
                .version(1)
                .occurredAt(Instant.now())
                .payload(payload)
                .build();
    }
}

