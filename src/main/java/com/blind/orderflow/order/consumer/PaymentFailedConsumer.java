package com.blind.orderflow.order.consumer;

import com.blind.orderflow.config.KafkaConfig;
import com.blind.orderflow.order.service.OrderService;
import com.blind.orderflow.shared.events.BaseEvent;
import com.blind.orderflow.shared.events.PaymentFailedPayload;
import com.blind.orderflow.shared.utils.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFailedConsumer {

    private final OrderService orderService;
    private final ObjectMapper mapper;


    @KafkaListener(topics = KafkaConfig.PAYMENT_FAILED_TOPIC,groupId = "order-group")
    public void handlePaymentFailed(BaseEvent<?> event) {

        PaymentFailedPayload payload =
                mapper.convertValue(event.getPayload(), PaymentFailedPayload.class);

        orderService.cancelOrder(payload.getOrderId(), "payment_failed")
                .doOnError(e -> Logger.error(
                        payload.getOrderId(),
                        "ORDER",
                        "ORDER_CANCEL_FAILED",
                        "ERROR",
                        "Failed to cancel order: " + e.getMessage()
                )).doOnSuccess(e -> Logger.info(
                        payload.getOrderId(),
                        "ORDER",
                        "ORDER_CANCELED",
                        "INFO",
                        "Order canceled successfully"
                ))
                .subscribe();
    }
}