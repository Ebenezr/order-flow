package com.blind.orderflow.report.controller;

import com.blind.orderflow.report.dto.DailySalesReport;
import com.blind.orderflow.report.dto.KitchenPerformanceReport;
import com.blind.orderflow.report.service.ReportService;
import com.blind.orderflow.shared.utils.apis.ApiResponse;
import com.blind.orderflow.shared.utils.apis.ResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/daily-sales")
    public Mono<ApiResponse<DailySalesReport>> getDailySalesReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate reportDate = (date != null) ? date : LocalDate.now();

        return reportService.getDailySalesReport(reportDate)
                .flatMap(report ->
                        ResponseFactory.getRequestRefId()
                                .flatMap(requestId ->
                                        ResponseFactory.success(report, requestId)
                                )
                );
    }

    @GetMapping("/kitchen-performance")
    public Mono<ApiResponse<KitchenPerformanceReport>> getKitchenPerformanceReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate reportDate = (date != null) ? date : LocalDate.now();

        return reportService.getKitchenPerformanceReport(reportDate)
                .flatMap(report ->
                        ResponseFactory.getRequestRefId()
                                .flatMap(requestId ->
                                        ResponseFactory.success(report, requestId)
                                )
                );
    }
}

