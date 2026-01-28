package com.angellos.trading.service.controller;

import com.angellos.shared.dto.OrderRequest;
import com.angellos.shared.record.Params;
import com.angellos.shared.record.Response;
import com.angellos.trading.service.service.OrderQueryService;
import com.angellos.trading.service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static com.angellos.shared.utility.AppConstants.*;
import static com.angellos.shared.utility.AppUtils.parseParams;

@RestController
@RequestMapping(TRADING_CONTEXT_PATH + "orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order Endpoints", description = "This contains all endpoints that are used to interact with the order model")
public class OrderController {

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;

    @Operation(
            description = "This endpoint is used to fetch all orders with optional filtering and pagination. Supports filtering by userId when provided in query parameters.",
            summary = "Endpoint to fetch all orders by given criteria",
            parameters = {
                    @Parameter(name = PARAM_PAGINATE, description = "Enable pagination", example = DEFAULT_PAGINATE, schema = @Schema(type = "string", defaultValue = DEFAULT_PAGINATE)),
                    @Parameter(name = PARAM_SEARCH, description = "Search term for filtering orders", example = DEFAULT_SEARCH, schema = @Schema(type = "string", defaultValue = DEFAULT_SEARCH)),
                    @Parameter(name = PARAM_SORT_DIR, description = "Sort direction (asc/desc)", example = DEFAULT_PAGE_SORT_DIR, schema = @Schema(type = "string", defaultValue = DEFAULT_PAGE_SORT_DIR)),
                    @Parameter(name = PARAM_SORT_BY, description = "Field to sort by", example = DEFAULT_PAGE_SORT_BY, schema = @Schema(type = "string", defaultValue = DEFAULT_PAGE_SORT_BY)),
                    @Parameter(name = PARAM_PAGE_SIZE, description = "Number of records per page", example = DEFAULT_PAGE_SIZE, schema = @Schema(type = "string", defaultValue = DEFAULT_PAGE_SIZE)),
                    @Parameter(name = PARAM_PAGE_NO, description = "Page number (1-indexed)", example = DEFAULT_PAGE_NUMBER, schema = @Schema(type = "string", defaultValue = DEFAULT_PAGE_NUMBER)),
                    @Parameter(name = PARAM_USER_ID, description = "Filter orders by user ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000", schema = @Schema(type = "string", format = "uuid"))
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Orders retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Response.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = Response.class))
            )
    })
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    @GetMapping
    public ResponseEntity<Response> findAllOrders(@RequestParam(defaultValue = "{}") Map<String, String> params) {
        log.info("Fetching all orders with params: {}", params);
        
        // Parse params to check for userId
        Params parsedParams = parseParams(params);
        
        // If userId is present, filter by user, otherwise get all
        if (parsedParams.userId() != null) {
            return orderQueryService.getUserOrders(parsedParams.userId());
        } else {
            return orderQueryService.getAllOrders();
        }
    }

    @Operation(
            description = "This endpoint is used to fetch a single order by its unique identifier (UUID).",
            summary = "Endpoint to fetch an order by ID"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Order retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Response.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found",
                    content = @Content(schema = @Schema(implementation = Response.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = Response.class))
            )
    })
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    @GetMapping("/{id}")
    public ResponseEntity<Response> findOrderById(
            @Parameter(description = "Order ID (UUID)", required = true, example = "550e8400-e29b-41d4-a716-446655440000", schema = @Schema(type = "string", format = "uuid"))
            @PathVariable UUID id) {
        log.info("Fetching order by id: {}", id);
        return orderQueryService.getOrderById(id);
    }

    @Operation(
            description = "This endpoint is used to fetch all orders for a specific trading symbol (e.g., AAPL, GOOGL).",
            summary = "Endpoint to fetch orders by trading symbol"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Orders retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Response.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = Response.class))
            )
    })
    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<Response> findOrdersBySymbol(
            @Parameter(description = "Trading symbol (e.g., AAPL, GOOGL)", required = true, example = "AAPL", schema = @Schema(type = "string", maxLength = 10))
            @PathVariable String symbol) {
        log.info("Fetching orders by symbol: {}", symbol);
        return orderQueryService.getOrdersBySymbol(symbol);
    }

    @Operation(
            description = "This endpoint is used to fetch all orders with a specific status (PENDING, EXECUTED, CANCELLED, REJECTED, PARTIALLY_EXECUTED).",
            summary = "Endpoint to fetch orders by status"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Orders retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Response.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = Response.class))
            )
    })
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    @GetMapping("/status/{status}")
    public ResponseEntity<Response> findOrdersByStatus(
            @Parameter(description = "Order status (PENDING, EXECUTED, CANCELLED, REJECTED, PARTIALLY_EXECUTED)", required = true, example = "PENDING", schema = @Schema(type = "string", allowableValues = {"PENDING", "EXECUTED", "CANCELLED", "REJECTED", "PARTIALLY_EXECUTED"}))
            @PathVariable String status) {
        log.info("Fetching orders by status: {}", status);
        return orderQueryService.getOrdersByStatus(status);
    }

    @Operation(
            description = "This endpoint is used to create a new trading order. The order will be persisted to the write model (PostgreSQL) and an event will be published to Kafka for event sourcing. " +
                    "Note: Requires authentication via JWT token. The user ID is automatically extracted from the JWT token claims.",
            summary = "Endpoint to create a new order"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Order created successfully",
                    content = @Content(schema = @Schema(implementation = Response.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - validation error",
                    content = @Content(schema = @Schema(implementation = Response.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = Response.class))
            )
    })
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<Response> createOrder(
            @Parameter(description = "Order request containing order details", required = true)
            @Valid @RequestBody OrderRequest orderRequest) {
        log.info("Creating new order for symbol: {}, type: {}",
                orderRequest.getSymbol(), orderRequest.getType());
        return orderService.createOrder(orderRequest);
    }

    @Operation(
            description = "This endpoint is used to update an existing order",
            summary = "Endpoint to update an existing order"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Order updated successfully",
                    content = @Content(schema = @Schema(implementation = Response.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - validation error",
                    content = @Content(schema = @Schema(implementation = Response.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found",
                    content = @Content(schema = @Schema(implementation = Response.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = Response.class))
            )
    })
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Response> updateOrder(
            @Parameter(description = "Order request containing updated order details", required = true)
            @Valid @RequestBody OrderRequest orderRequest,
            @Parameter(description = "Order ID (UUID) to update", required = true, example = "550e8400-e29b-41d4-a716-446655440000", schema = @Schema(type = "string", format = "uuid"))
            @PathVariable UUID id) {
        log.info("Updating order: {}", id);
        return orderService.updateOrder(id, orderRequest);
    }

    @Operation(
            description = "This endpoint is used to cancel an existing order. The order status will be updated to CANCELLED in the write model (PostgreSQL) and a cancellation event will be published to Kafka.",
            summary = "Endpoint to cancel an order"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Order cancelled successfully",
                    content = @Content(schema = @Schema(implementation = Response.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found",
                    content = @Content(schema = @Schema(implementation = Response.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - order cannot be cancelled (e.g., already executed)",
                    content = @Content(schema = @Schema(implementation = Response.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = Response.class))
            )
    })
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Response> cancelOrder(
            @Parameter(description = "Order ID (UUID) to cancel", required = true, example = "550e8400-e29b-41d4-a716-446655440000", schema = @Schema(type = "string", format = "uuid"))
            @PathVariable UUID id) {
        log.info("Cancelling order: {}", id);
        return orderService.cancelOrder(id);
    }

    @Operation(
            description = "This endpoint is used to execute an order (full or partial execution). The order status will be updated to EXECUTED or PARTIALLY_EXECUTED, and an execution event will be published to Kafka.",
            summary = "Endpoint to execute an order"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Order executed successfully",
                    content = @Content(schema = @Schema(implementation = Response.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Order not found",
                    content = @Content(schema = @Schema(implementation = Response.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - validation error (e.g., order already fully executed, invalid execution parameters)",
                    content = @Content(schema = @Schema(implementation = Response.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = Response.class))
            )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/execute")
    public ResponseEntity<Response> executeOrder(
            @Parameter(description = "Order ID (UUID) to execute", required = true, example = "550e8400-e29b-41d4-a716-446655440000", schema = @Schema(type = "string", format = "uuid"))
            @PathVariable UUID id,
            @Parameter(description = "Execution price per unit", required = true, example = "150.50", schema = @Schema(type = "number", format = "decimal"))
            @RequestParam BigDecimal executionPrice,
            @Parameter(description = "Execution quantity (can be partial or full)", required = true, example = "5", schema = @Schema(type = "number", format = "decimal"))
            @RequestParam BigDecimal executionQuantity) {
        log.info("Executing order: {} with price: {}, quantity: {}", id, executionPrice, executionQuantity);
        return orderService.executeOrder(id, executionPrice, executionQuantity);
    }

    @Operation(
            description = "This endpoint is used to fetch order history with pagination and filtering. " +
                    "Queries PostgreSQL (write model) for historical data. " +
                    "Supports filtering by userId, status, symbol (via search), and date range. " +
                    "Results are sorted by creation date (newest first) and paginated.",
            summary = "Endpoint to fetch order history with pagination and filters",
            parameters = {
                    @Parameter(name = PARAM_PAGINATE, description = "Enable pagination", example = DEFAULT_PAGINATE, schema = @Schema(type = "string", defaultValue = DEFAULT_PAGINATE)),
                    @Parameter(name = PARAM_SEARCH, description = "Search by symbol", example = "AAPL", schema = @Schema(type = "string")),
                    @Parameter(name = PARAM_STATUS, description = "Filter by order status (PENDING, EXECUTED, CANCELLED, etc.)", example = "EXECUTED", schema = @Schema(type = "string")),
                    @Parameter(name = PARAM_SORT_DIR, description = "Sort direction (asc/desc)", example = DEFAULT_PAGE_SORT_DIR, schema = @Schema(type = "string", defaultValue = DEFAULT_PAGE_SORT_DIR)),
                    @Parameter(name = PARAM_SORT_BY, description = "Field to sort by", example = DEFAULT_PAGE_SORT_BY, schema = @Schema(type = "string", defaultValue = DEFAULT_PAGE_SORT_BY)),
                    @Parameter(name = PARAM_PAGE_SIZE, description = "Number of records per page (max 100)", example = DEFAULT_PAGE_SIZE, schema = @Schema(type = "string", defaultValue = DEFAULT_PAGE_SIZE)),
                    @Parameter(name = PARAM_PAGE_NO, description = "Page number (1-indexed)", example = DEFAULT_PAGE_NUMBER, schema = @Schema(type = "string", defaultValue = DEFAULT_PAGE_NUMBER)),
                    @Parameter(name = PARAM_USER_ID, description = "Filter orders by user ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000", schema = @Schema(type = "string", format = "uuid")),
                    @Parameter(name = START_DATE, description = "Start date for date range filter (ISO format: yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss)", example = "2026-01-01", schema = @Schema(type = "string")),
                    @Parameter(name = END_DATE, description = "End date for date range filter (ISO format: yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss)", example = "2026-01-31", schema = @Schema(type = "string"))
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Order history retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Response.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - invalid filter parameters",
                    content = @Content(schema = @Schema(implementation = Response.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = Response.class))
            )
    })
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    public ResponseEntity<Response> getOrderHistory(@RequestParam(defaultValue = "{}") Map<String, String> params) {
        log.info("Fetching order history with params: {}", params);
        Params parsedParams = parseParams(params);
        return orderQueryService.getOrderHistory(parsedParams);
    }
}
