package com.blind.orderflow.inventory.consumer;

import com.blind.orderflow.config.KafkaConfig;
import com.blind.orderflow.order.service.OrderService;
import com.blind.orderflow.shared.events.BaseEvent;
import com.blind.orderflow.shared.events.InventoryReservedPayload;
import com.blind.orderflow.shared.idempotency.IdempotencyService;
import com.blind.orderflow.shared.utils.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class InventoryReservedConsumer {

    private final OrderService orderService;
    private final ObjectMapper mapper;
    private final IdempotencyService idempotencyService;

    private static final Duration PAYMENT_TIMEOUT = Duration.ofMinutes(3);

    @KafkaListener(topics = KafkaConfig.INVENTORY_RESERVED_TOPIC)
    public void handleInventoryReserved(BaseEvent<?> event) {

        InventoryReservedPayload payload =
                mapper.convertValue(event.getPayload(), InventoryReservedPayload.class);


        String orderId = payload.getOrderId();
        String correlationId = event.getCorrelationId();
        String eventId = String.valueOf(event.getEventId());


        idempotencyService.executeOnceVoid(
                eventId,
                correlationId,
                "ORDER",
                () -> {
                    Logger.info(
                            correlationId,
                            "ORDER",
                            "AWAITING_PAYMENT",
                            "INFO",
                            "Inventory reserved. Waiting for client payment (timeout=" + PAYMENT_TIMEOUT.toMinutes() + "min)"
                    );


                    Mono.delay(PAYMENT_TIMEOUT)
                            .then(orderService.cancelOrderIfStillPending(orderId, correlationId))
                            .contextWrite(ctx -> ctx.put("correlationId", correlationId))
                            .subscribe();

                    return Mono.empty();
                }
        )
                .doOnError(e -> Logger.error(
                        correlationId,
                        "ORDER",
                        "INVENTORY_RESERVED_ERROR",
                        "ERROR",
                        e.getMessage()
                ))
                .contextWrite(ctx -> ctx.put("correlationId", correlationId))
                .subscribe();
    }

}