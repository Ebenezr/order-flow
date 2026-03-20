package com.blind.orderflow.payment.consumer;


import com.blind.orderflow.config.KafkaConfig;
import com.blind.orderflow.inventory.service.InventoryService;
import com.blind.orderflow.order.service.OrderService;
import com.blind.orderflow.shared.events.BaseEvent;
import com.blind.orderflow.shared.events.PaymentFailedPayload;
import com.blind.orderflow.shared.utils.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentFailedConsumer {

    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final ObjectMapper mapper;

    @KafkaListener(topics = KafkaConfig.PAYMENT_FAILED_TOPIC)
    public void handlePaymentFailed(BaseEvent<?> event) {

        PaymentFailedPayload payload =
                mapper.convertValue(event.getPayload(), PaymentFailedPayload.class);

        String orderId = payload.getOrderId();
        String correlationId = event.getCorrelationId();

        inventoryService.releaseReservation(orderId)
                .then(orderService.cancelOrder(orderId, payload.getReason(),correlationId))
                .doOnSuccess(v ->
                        Logger.info(correlationId, "PAYMENT", "EVENT_FAILURE_FLOW_COMPLETE", "SUCCESS",
                                "Inventory released and order cancelled")
                )
                .doOnError(e ->
                        Logger.error(correlationId, "PAYMENT", "EVENT_FAILURE_FLOW_ERROR", "ERROR",
                                e.getMessage())
                )
                .contextWrite(ctx -> ctx.put("correlationId", correlationId))
                .subscribe();
    }
}