package com.blind.orderflow.report.controller;

import com.blind.orderflow.report.dto.CancellationReport;
import com.blind.orderflow.report.dto.CategoryRevenue;
import com.blind.orderflow.report.dto.DailySalesReport;
import com.blind.orderflow.report.dto.KitchenPerformanceReport;
import com.blind.orderflow.report.dto.MonthlySummaryReport;
import com.blind.orderflow.report.dto.PaymentReport;
import com.blind.orderflow.report.dto.ProcessingTimeReport;
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
import java.time.YearMonth;
import java.util.List;

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

    @GetMapping("/payments")
    public Mono<ApiResponse<PaymentReport>> getPaymentReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate reportDate = (date != null) ? date : LocalDate.now();

        return reportService.getPaymentReport(reportDate)
                .flatMap(report ->
                        ResponseFactory.getRequestRefId()
                                .flatMap(requestId ->
                                        ResponseFactory.success(report, requestId)
                                )
                );
    }

    @GetMapping("/cancellations")
    public Mono<ApiResponse<CancellationReport>> getCancellationReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate reportDate = (date != null) ? date : LocalDate.now();

        return reportService.getCancellationReport(reportDate)
                .flatMap(report ->
                        ResponseFactory.getRequestRefId()
                                .flatMap(requestId ->
                                        ResponseFactory.success(report, requestId)
                                )
                );
    }

    @GetMapping("/processing-times")
    public Mono<ApiResponse<ProcessingTimeReport>> getProcessingTimeReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate reportDate = (date != null) ? date : LocalDate.now();

        return reportService.getProcessingTimeReport(reportDate)
                .flatMap(report ->
                        ResponseFactory.getRequestRefId()
                                .flatMap(requestId ->
                                        ResponseFactory.success(report, requestId)
                                )
                );
    }

    @GetMapping("/revenue-by-category")
    public Mono<ApiResponse<List<CategoryRevenue>>> getRevenueByCategoryReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate reportDate = (date != null) ? date : LocalDate.now();

        return reportService.getRevenueByCategoryReport(reportDate)
                .flatMap(report ->
                        ResponseFactory.getRequestRefId()
                                .flatMap(requestId ->
                                        ResponseFactory.success(report, requestId)
                                )
                );
    }

    @GetMapping("/monthly-summary")
    public Mono<ApiResponse<MonthlySummaryReport>> getMonthlySummaryReport(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        YearMonth yearMonth = (year != null && month != null)
                ? YearMonth.of(year, month)
                : YearMonth.now();

        return reportService.getMonthlySummaryReport(yearMonth)
                .flatMap(report ->
                        ResponseFactory.getRequestRefId()
                                .flatMap(requestId ->
                                        ResponseFactory.success(report, requestId)
                                )
                );
    }
}

