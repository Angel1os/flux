package com.angellos.portfolio.service.controller;

import com.angellos.portfolio.service.service.PositionService;
import com.angellos.shared.record.Response;
import com.angellos.shared.utility.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}/positions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Position Endpoints", description = "REST API endpoints for position management")
public class PositionController {

    private final PositionService positionService;

    @Operation(
            description = "Get all positions for a portfolio",
            summary = "Get positions"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Positions retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Portfolio not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    public ResponseEntity<Response> getPositions(
            @Parameter(description = "Portfolio ID", required = true)
            @PathVariable UUID portfolioId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Fetching positions for portfolio: {} for user: {}", portfolioId, userId);
        return positionService.getPositions(portfolioId, userId);
    }

    @Operation(
            description = "Get position by symbol",
            summary = "Get position"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Position retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Position not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{symbol}")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    public ResponseEntity<Response> getPosition(
            @Parameter(description = "Portfolio ID", required = true)
            @PathVariable UUID portfolioId,
            @Parameter(description = "Trading symbol", required = true, example = "AAPL")
            @PathVariable String symbol) {
        UUID userId = SecurityUtils.getCurrentUserId();
        log.info("Fetching position: {} for portfolio: {} for user: {}", symbol, portfolioId, userId);
        return positionService.getPosition(portfolioId, userId, symbol);
    }
}
