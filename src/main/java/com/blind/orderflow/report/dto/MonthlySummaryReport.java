package com.blind.orderflow.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummaryReport {

    private String period;
    private Double grossSales;
    private Double netSales;
    private Double vatCollected;
}

