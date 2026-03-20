package com.blind.orderflow.inventory.consumer;

import com.blind.orderflow.config.KafkaConfig;
import com.blind.orderflow.order.service.OrderService;
import com.blind.orderflow.shared.events.BaseEvent;
import com.blind.orderflow.shared.events.InventoryFailedPayload;
import com.blind.orderflow.shared.utils.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryFailedConsumer {

    private final OrderService orderService;
    private final ObjectMapper mapper;


    @KafkaListener(topics = KafkaConfig.INVENTORY_FAILED_TOPIC)
    public void handleInventoryFailed(BaseEvent<?> event) {

        InventoryFailedPayload payload =
                mapper.convertValue(event.getPayload(), InventoryFailedPayload.class);

        String orderId = payload.getOrderId();
        String correlationId = event.getCorrelationId();



        orderService.cancelOrder(orderId, payload.getReason(),correlationId)
                .doOnError(
                        throwable ->
                            Logger.info(
                                    correlationId,
                                    "ORDER",
                                    "EVENT_ORDER_CANCEL_CALL_FAILED",
                                    "SUCCESS",
                                    "Order cancelled due to inventory failure"
                            )
                )
                .doOnSuccess(
                        order ->
                                Logger.info(
                                        correlationId,
                                        "ORDER",
                                        "EVENT_ORDER_CANCEL_CALL_SUCCESS",
                                        "SUCCESS",
                                        "Order cancelled due to inventory failure"
                                )

                )
                .contextWrite(ctx -> ctx.put("correlationId", correlationId))
                .subscribe();
    }
}
