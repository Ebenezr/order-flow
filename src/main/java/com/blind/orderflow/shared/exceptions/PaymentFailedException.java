package com.blind.orderflow.shared.exceptions;

public class PaymentFailedException extends BusinessException {

    public PaymentFailedException(String reason) {
        super("Payment failed: " + reason, "PAYMENT_FAILED");
    }
}