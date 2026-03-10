package com.blind.orderflow.inventory.consumer;

import com.blind.orderflow.config.KafkaConfig;
import com.blind.orderflow.order.service.OrderService;
import com.blind.orderflow.shared.events.BaseEvent;
import com.blind.orderflow.shared.events.InventoryFailedPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryFailedConsumer {

    private final OrderService orderService;
    private final ObjectMapper mapper;


    @KafkaListener(topics = KafkaConfig.INVENTORY_FAILED_TOPIC,groupId = "inventory-group")
    public void handleInventoryFailed(BaseEvent<?> event) {

        InventoryFailedPayload payload =
                mapper.convertValue(event.getPayload(), InventoryFailedPayload.class);

        orderService.cancelOrder(payload.getOrderId()).subscribe();
    }
}
