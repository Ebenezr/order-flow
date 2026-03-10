package com.blind.orderflow.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KitchenPerformanceReport {

    private LocalDate date;
    private Long totalKitchenOrders;
    private Double avgPrepTimeMinutes;
    private Double minPrepTimeMinutes;
    private Double maxPrepTimeMinutes;
    private List<SlowItem> slowItems;
}

