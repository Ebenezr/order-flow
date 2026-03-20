package com.blind.orderflow.order.consumer;

import com.blind.orderflow.config.KafkaConfig;
import com.blind.orderflow.inventory.service.InventoryService;
import com.blind.orderflow.shared.events.BaseEvent;
import com.blind.orderflow.shared.events.OrderCreatedPayload;
import com.blind.orderflow.shared.utils.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final InventoryService inventoryService;

    private final ObjectMapper mapper;

    @KafkaListener(topics = KafkaConfig.ORDER_CREATED_TOPIC)
    public void handleOrderCreated(BaseEvent<?> event) {

        OrderCreatedPayload payload =
                mapper.convertValue(event.getPayload(), OrderCreatedPayload.class);

        String correlationId = event.getCorrelationId();
        String orderId = payload.getOrderId();

        inventoryService.reserveStock(orderId,correlationId)
                .doOnSuccess(
                        v -> Logger.info(
                                correlationId,
                                "INVENTORY",
                                "EVENT_SUCCESS_RESERVE_STOCK_CALLED",
                                "INFO",
                                "Stock reserved successfully, proceeding to payment"
                        )
                ).doOnError(
                        e -> Logger.error(
                                correlationId,
                                "INVENTORY",
                                "EVENT_ERROR_RESERVE_STOCK_CALL_FAILED",
                                "ERROR",
                                "Stock reservation failed: " + e.getMessage()
                        )
                )
                .contextWrite(ctx -> ctx.put("correlationId", correlationId))
                .subscribe();
    }
}
