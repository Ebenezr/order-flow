package com.blind.orderflow.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReport {

    private LocalDate date;
    private Long successfulPayments;
    private Long failedPayments;
    private Double failureRate;
}

