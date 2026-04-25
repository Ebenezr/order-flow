package com.blind.orderflow.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentRequest {

    @NotBlank(message = "Payment method is required")
    private String method; // MPESA or CARD

    // MPESA fields
    private String phone;

    // CARD fields
    private String cardNumber;
    private String expiry;
    private String cvv;

    @NotNull(message = "simulateSuccess flag is required")
    private Boolean simulateSuccess;
}

