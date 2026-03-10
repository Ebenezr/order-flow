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
public class DailySalesReport {

    private LocalDate date;
    private Long totalOrders;
    private Double totalRevenue;
    private Double vatCollected;
    private List<TopSellingItem> topSellingItems;
}

