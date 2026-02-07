package com.angellos.portfolio.service.controller;

import com.angellos.portfolio.service.service.TransactionService;
import com.angellos.shared.record.Response;
import com.angellos.shared.utility.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}/transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transaction Endpoints", description = "REST API endpoints for transaction history")
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(
            description = "Get transaction history for a portfolio with pagination",
            summary = "Get transactions"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Portfolio not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    public ResponseEntity<Response> getTransactions(
            @Parameter(description = "Portfolio ID", required = true)
            @PathVariable UUID portfolioId,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        log.info("Fetching transactions for portfolio: {} for user: {}", portfolioId, userId);
        return transactionService.getTransactions(portfolioId, userId, pageable);
    }

    @Operation(
            description = "Get transactions within a date range",
            summary = "Get transactions by date range"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Portfolio not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    public ResponseEntity<Response> getTransactionsByDateRange(
            @Parameter(description = "Portfolio ID", required = true)
            @PathVariable UUID portfolioId,
            @Parameter(description = "Start date (ISO format)", required = true, example = "2026-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)", required = true, example = "2026-01-31T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Fetching transactions for portfolio: {} for user: {} from {} to {}", 
                portfolioId, userId, startDate, endDate);
        return transactionService.getTransactionsByDateRange(portfolioId, userId, startDate, endDate);
    }
}
