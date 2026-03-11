package com.blind.orderflow.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationReport {

    private LocalDate date;
    private Long cancelledOrders;
    private Map<String, Long> reasons;
}

