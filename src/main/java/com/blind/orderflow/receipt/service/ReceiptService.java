package com.blind.orderflow.receipt.service;

import com.blind.orderflow.order.entity.Order;
import com.blind.orderflow.order.entity.OrderItem;
import com.blind.orderflow.receipt.model.ReceiptLine;
import com.blind.orderflow.receipt.util.ReceiptFormatter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReceiptService {

    public void printReceipt(Order order, List<OrderItem> items, String transactionId) {

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

        String receipt =
                ReceiptFormatter.format(
                        "Blind Restaurant",
                        "KE12345678",
                        "RCPT-" + order.getOrderId(),
                        order.getOrderId(),
                        transactionId,
                        lines,
                        order.getSubtotal(),
                        order.getVat(),
                        order.getServiceCharge(),
                        order.getDiscount(),
                        order.getTotalAmount()
                );

        System.out.println(receipt);
    }
}