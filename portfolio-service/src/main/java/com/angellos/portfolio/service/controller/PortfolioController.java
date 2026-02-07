package com.angellos.portfolio.service.controller;

import com.angellos.portfolio.service.service.PortfolioService;
import com.angellos.shared.record.Response;
import com.angellos.shared.utility.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Portfolio Endpoints", description = "REST API endpoints for portfolio management")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @Operation(
            description = "Create a new portfolio for the authenticated user",
            summary = "Create portfolio"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Portfolio created successfully"),
            @ApiResponse(responseCode = "409", description = "Portfolio with name already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    public ResponseEntity<Response> createPortfolio(
            @Parameter(description = "Portfolio name", required = true, example = "Main Portfolio")
            @RequestParam String name) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Creating portfolio for user: {}, name: {}", userId, name);
        return portfolioService.createPortfolio(userId, name);
    }

    @Operation(
            description = "Get portfolio by ID",
            summary = "Get portfolio"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Portfolio retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Portfolio not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    public ResponseEntity<Response> getPortfolio(
            @Parameter(description = "Portfolio ID", required = true)
            @PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Fetching portfolio: {} for user: {}", id, userId);
        return portfolioService.getPortfolio(id, userId);
    }

    @Operation(
            description = "Get all portfolios for the authenticated user",
            summary = "Get all portfolios"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Portfolios retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    public ResponseEntity<Response> getAllPortfolios() {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Fetching all portfolios for user: {}", userId);
        return portfolioService.getAllPortfolios(userId);
    }

    @Operation(
            description = "Update portfolio name",
            summary = "Update portfolio"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Portfolio updated successfully"),
            @ApiResponse(responseCode = "404", description = "Portfolio not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    public ResponseEntity<Response> updatePortfolio(
            @Parameter(description = "Portfolio ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "New portfolio name", required = true)
            @RequestParam String name) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Updating portfolio: {} for user: {}", id, userId);
        return portfolioService.updatePortfolio(id, userId, name);
    }

    @Operation(
            description = "Delete portfolio",
            summary = "Delete portfolio"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Portfolio deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Portfolio not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    public ResponseEntity<Response> deletePortfolio(
            @Parameter(description = "Portfolio ID", required = true)
            @PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Deleting portfolio: {} for user: {}", id, userId);
        return portfolioService.deletePortfolio(id, userId);
    }

    @Operation(
            description = "Get portfolio summary with positions and performance metrics",
            summary = "Get portfolio summary"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Portfolio summary retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Portfolio not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}/summary")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    public ResponseEntity<Response> getPortfolioSummary(
            @Parameter(description = "Portfolio ID", required = true)
            @PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Fetching portfolio summary: {} for user: {}", id, userId);
        return portfolioService.getPortfolioSummary(id, userId);
    }

    @Operation(
            description = "Deposit cash to portfolio",
            summary = "Deposit cash"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cash deposited successfully"),
            @ApiResponse(responseCode = "404", description = "Portfolio not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{id}/deposit")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    public ResponseEntity<Response> depositCash(
            @Parameter(description = "Portfolio ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Amount to deposit", required = true, example = "1000.00")
            @RequestParam BigDecimal amount) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Depositing cash: {} to portfolio: {} for user: {}", amount, id, userId);
        return portfolioService.depositCash(id, userId, amount);
    }

    @Operation(
            description = "Withdraw cash from portfolio",
            summary = "Withdraw cash"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cash withdrawn successfully"),
            @ApiResponse(responseCode = "400", description = "Insufficient cash balance"),
            @ApiResponse(responseCode = "404", description = "Portfolio not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    public ResponseEntity<Response> withdrawCash(
            @Parameter(description = "Portfolio ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Amount to withdraw", required = true, example = "500.00")
            @RequestParam BigDecimal amount) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Withdrawing cash: {} from portfolio: {} for user: {}", amount, id, userId);
        return portfolioService.withdrawCash(id, userId, amount);
    }
}
