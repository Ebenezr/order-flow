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
public class ProcessingTimeReport {

    private LocalDate date;
    private Double avgInventoryReserveTimeMs;
    private Double avgPaymentProcessingMs;
    private Double avgKitchenPrepMinutes;
}

