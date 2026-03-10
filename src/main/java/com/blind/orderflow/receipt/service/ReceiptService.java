package com.blind.orderflow.receipt.service;

import com.blind.orderflow.order.entity.Order;
import com.blind.orderflow.order.entity.OrderItem;
import com.blind.orderflow.receipt.entity.Receipt;
import com.blind.orderflow.receipt.model.ReceiptLine;
import com.blind.orderflow.receipt.repository.ReceiptRepository;
import com.blind.orderflow.receipt.util.ReceiptFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final ReceiptRepository receiptRepository;

    private static final String BUSINESS_NAME = "Blind Restaurant";
    private static final String VAT_NUMBER = "KE12345678";

    public Mono<Receipt> printReceipt(Order order, List<OrderItem> items, String transactionId) {

        String receiptNumber = "RCPT-" + order.getOrderId();

        List<ReceiptLine> lines =
                items.stream()
                        .map(i ->
                                new ReceiptLine(
                                        i.getProductName(),
                                        i.getQuantity(),
                                        i.getPrice()
                                )
                        )
                        .collect(Collectors.toList());

        String formatted =
                ReceiptFormatter.format(
                        BUSINESS_NAME,
                        VAT_NUMBER,
                        receiptNumber,
                        order.getOrderId(),
                        transactionId,
                        lines,
                        order.getSubtotal(),
                        order.getVat(),
                        order.getServiceCharge(),
                        order.getDiscount(),
                        order.getTotalAmount()
                );

        System.out.println(formatted);

        Receipt receipt = Receipt.builder()
                .receiptNumber(receiptNumber)
                .orderId(order.getOrderId())
                .transactionId(transactionId)
                .subtotal(order.getSubtotal())
                .vat(order.getVat())
                .serviceCharge(order.getServiceCharge())
                .discount(order.getDiscount())
                .total(order.getTotalAmount())
                .vatNumber(VAT_NUMBER)
                .businessName(BUSINESS_NAME)
                .createdAt(LocalDateTime.now())
                .build();

        return receiptRepository.save(receipt);
    }
}