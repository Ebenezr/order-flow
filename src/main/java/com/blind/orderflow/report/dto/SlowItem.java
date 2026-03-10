package com.blind.orderflow.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlowItem {

    private String productId;
    private String productName;
    private String orderId;
    private Long prepTimeMinutes;
}

