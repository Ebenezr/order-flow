package com.blind.orderflow.order.consumer;

import com.blind.orderflow.config.KafkaConfig;
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
    private final ObjectMapper mapper;

    @KafkaListener(topics = KafkaConfig.PAYMENT_COMPLETED_TOPIC,groupId = "order-group")
    public void handlePaymentCompleted(BaseEvent<?> event) {

        PaymentCompletedPayload payload =
                mapper.convertValue(event.getPayload(), PaymentCompletedPayload.class);

        orderService.confirmOrder(payload.getOrderId()).subscribe();

        orderService.getOrder(payload.getOrderId())
                .zipWith(orderService.getOrderItems(payload.getOrderId()).collectList())
                .doOnNext(tuple -> {

                    Order order = tuple.getT1();
                    List<OrderItem> items = tuple.getT2();

                    receiptService.printReceipt(order, items, payload.getTransactionId());
                }).doOnError(e -> Logger.error(
                        payload.getOrderId(),
                        "ORDER",
                        "RECEIPT_PRINT_FAILED",
                        "ERROR",
                        "Failed to print receipt: " + e.getMessage()
                )).doOnSuccess(e -> Logger.info(
                        payload.getOrderId(),
                        "ORDER",
                        "RECEIPT_PRINTED",
                        "INFO",
                        "Receipt printed successfully"
                ))
                .subscribe();
    }
}