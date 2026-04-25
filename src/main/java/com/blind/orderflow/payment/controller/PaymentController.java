package com.blind.orderflow.payment.controller;

import com.blind.orderflow.payment.dto.PaymentRequest;
import com.blind.orderflow.payment.entity.PaymentTransaction;
import com.blind.orderflow.payment.service.PaymentService;
import com.blind.orderflow.shared.utils.apis.ApiResponse;
import com.blind.orderflow.shared.utils.apis.ResponseFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/v1/payments/{orderId}
     *
     * Accepts a payment request from the frontend:
     * {
     *   "method": "MPESA" | "CARD",
     *   "phone": "+254...",          // required for MPESA
     *   "cardNumber": "4111...",     // required for CARD
     *   "expiry": "12/28",          // required for CARD
     *   "cvv": "123",               // required for CARD
     *   "simulateSuccess": true
     * }
     */
    @PostMapping("/{orderId}")
    public Mono<ApiResponse<PaymentTransaction>> pay(
            @PathVariable String orderId,
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader(value = "X-Correlation-Id", required = true) String correlationId
    ) {
        return paymentService.processPayment(orderId, request, correlationId)
                .flatMap(tx ->
                        ResponseFactory.getRequestRefId()
                                .flatMap(requestId -> ResponseFactory.success(tx, requestId))
                );
    }
}

