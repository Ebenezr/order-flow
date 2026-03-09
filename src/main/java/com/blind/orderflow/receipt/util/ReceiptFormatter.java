package com.blind.orderflow.receipt.util;

import com.blind.orderflow.receipt.model.ReceiptLine;

import java.time.LocalDateTime;
import java.util.List;

public class ReceiptFormatter {

    public static String format(
            String businessName,
            String vatNumber,
            String receiptNumber,
            String orderId,
            String transactionId,
            List<ReceiptLine> items,
            double subtotal,
            double vat,
            double serviceCharge,
            double discount,
            double total) {

        StringBuilder receipt = new StringBuilder();

        receipt.append("\n--------------------------------\n");
        receipt.append("        ").append(businessName).append("\n");
        receipt.append("        VAT: ").append(vatNumber).append("\n");
        receipt.append("--------------------------------\n");

        receipt.append("Receipt: ").append(receiptNumber).append("\n");
        receipt.append("Order: ").append(orderId).append("\n");
        receipt.append("Date: ").append(LocalDateTime.now()).append("\n");
        receipt.append("\nItems\n");
        receipt.append("--------------------------------\n");

        for (ReceiptLine item : items) {

            receipt.append(
                    String.format(
                            "%-15s %2d x %.2f\n",
                            item.getItemName(),
                            item.getQuantity(),
                            item.getPrice()
                    )
            );
        }

        receipt.append("--------------------------------\n");

        receipt.append(String.format("Subtotal        %.2f\n", subtotal));
        receipt.append(String.format("VAT (15%%)       %.2f\n", vat));
        receipt.append(String.format("Service Charge  %.2f\n", serviceCharge));
        receipt.append(String.format("Discount        %.2f\n", discount));

        receipt.append("--------------------------------\n");
        receipt.append(String.format("TOTAL           %.2f\n", total));

        receipt.append("--------------------------------\n");
        receipt.append("Payment Ref: ").append(transactionId).append("\n");
        receipt.append("--------------------------------\n");
        receipt.append("       THANK YOU\n");
        receipt.append("--------------------------------\n");

        return receipt.toString();
    }
}