package com.blind.orderflow.payment.consumer;

import com.blind.orderflow.config.KafkaConfig;
import com.blind.orderflow.inventory.service.InventoryService;
import com.blind.orderflow.kitchen.service.KitchenService;
import com.blind.orderflow.order.entity.Order;
import com.blind.orderflow.order.entity.OrderItem;
import com.blind.orderflow.order.service.OrderService;
import com.blind.orderflow.receipt.service.ReceiptService;
import com.blind.orderflow.shared.events.BaseEvent;
import com.blind.orderflow.shared.events.PaymentCompletedPayload;
import com.blind.orderflow.shared.utils.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedConsumer {

    private final OrderService orderService;
    private final ReceiptService receiptService;
    private final KitchenService kitchenService;
    private final InventoryService inventoryService;
    private final ObjectMapper mapper;

    @KafkaListener(topics = KafkaConfig.PAYMENT_COMPLETED_TOPIC)
    public void handlePaymentCompleted(BaseEvent<?> event) {

        PaymentCompletedPayload payload =
                mapper.convertValue(event.getPayload(), PaymentCompletedPayload.class);

        String orderId = payload.getOrderId();
        String correlationId = event.getCorrelationId();

        inventoryService.confirmReservation(orderId,correlationId)
                .then(orderService.confirmOrder(orderId))
                        .then(kitchenService.createKitchenOrder(orderId))
                .then(
                        orderService.getOrder(orderId)
                                .zipWith(orderService.getOrderItems(orderId).collectList())
                                .flatMap(tuple -> {

                                    Order order = tuple.getT1();
                                    List<OrderItem> items = tuple.getT2();

                                    return receiptService.printReceipt(
                                            order,
                                            items,
                                            payload.getTransactionId()
                                    );
                                })
                )
                .doOnSuccess(v ->
                        Logger.info(correlationId, "ORDER", "EVENT_PAYMENT_FLOW_COMPLETE", "SUCCESS",
                                "Order fully processed after payment")
                )

                .doOnError(e ->
                        Logger.error(correlationId, "ORDER", "EVENT_PAYMENT_FLOW_ERROR", "ERROR",
                                e.getMessage())
                )
                .contextWrite(ctx -> ctx.put("correlationId", correlationId))
                .subscribe();
    }
}