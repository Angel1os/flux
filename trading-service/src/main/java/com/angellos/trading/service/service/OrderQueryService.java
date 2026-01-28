package com.angellos.trading.service.service;

import com.angellos.shared.enums.OrderStatus;
import com.angellos.shared.record.Params;
import com.angellos.shared.record.Response;
import com.angellos.trading.service.domain.model.Order;
import com.angellos.trading.service.domain.readmodel.OrderReadModel;
import com.angellos.trading.service.mapper.OrderReadModelMapper;
import com.angellos.trading.service.repository.OrderReadRepository;
import com.angellos.trading.service.repository.OrderRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.angellos.shared.enums.StatusCode.*;
import static com.angellos.shared.utility.AppUtils.*;

/**
 * CQRS Read Service - Handles all read operations for orders.
 * Uses Redis-backed read models for fast queries.
 * 
 * This is separate from OrderService (write side) to implement CQRS pattern.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderQueryService {

    private final OrderReadRepository orderReadRepository;
    private final OrderRepository orderRepository; // For querying write model (PostgreSQL) for history
    private final OrderReadModelMapper orderReadModelMapper;

    /**
     * Get all orders from read model (Redis)
     */
    public ResponseEntity<Response> getAllOrders() {
        log.info("Fetching all orders from read model");
        Response response;

        try {
            Iterable<OrderReadModel> orders = orderReadRepository.findAll();
            List<OrderReadModel> orderList = (List<OrderReadModel>) orders;
            log.info("Successfully fetched {} orders from read model", orderList.size());
            response = getResponse("Orders fetched successfully", HttpStatus.OK, SUCCESS, orderList);
            return response.toResponseEntity();
        } catch (Exception e) {
            log.error("Error fetching all orders from read model: {}", e.getMessage(), e);
            response = getResponse("Error fetching orders: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, null);
            return response.toResponseEntity();
        }
    }

    /**
     * Get order by ID from read model (Redis)
     * Uses caching for frequently accessed orders
     */
    @Cacheable(value = "orders", key = "#orderId")
    public ResponseEntity<Response> getOrderById(UUID orderId) {
        log.info("Fetching order by id from read model: {}", orderId);
        Response response;

        try {
            OrderReadModel order = orderReadRepository.findById(orderId)
                    .orElse(null);

            if (order != null) {
                log.info("Successfully fetched order from read model: {}", orderId);
                response = getResponse("Order fetched successfully", HttpStatus.OK, SUCCESS, order);
                return response.toResponseEntity();
            } else {
                log.warn("Order not found in read model: {}", orderId);
                response = getResponse("Order not found", HttpStatus.NOT_FOUND, CLIENT_ERROR, null);
                return response.toResponseEntity();
            }
        } catch (Exception e) {
            log.error("Error fetching order by id {} from read model: {}", orderId, e.getMessage(), e);
            response = getResponse("Error fetching order: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, null);
            return response.toResponseEntity();
        }
    }

    /**
     * Get all orders for a specific user from read model (Redis)
     */
    @Cacheable(value = "userOrders", key = "#userId")
    public ResponseEntity<Response> getUserOrders(UUID userId) {
        log.info("Fetching orders for user from read model: {}", userId);
        Response response;

        try {
            List<OrderReadModel> orders = orderReadRepository.findByUserId(userId);
            log.info("Successfully fetched {} orders for user from read model: {}", orders.size(), userId);
            response = getResponse("User orders fetched successfully", HttpStatus.OK, SUCCESS, orders);
            return response.toResponseEntity();
        } catch (Exception e) {
            log.error("Error fetching orders for user {} from read model: {}", userId, e.getMessage(), e);
            response = getResponse("Error fetching user orders: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, null);
            return response.toResponseEntity();
        }
    }

    /**
     * Get all orders for a specific symbol from read model (Redis)
     */
    public ResponseEntity<Response> getOrdersBySymbol(String symbol) {
        log.info("Fetching orders by symbol from read model: {}", symbol);
        Response response;

        try {
            List<OrderReadModel> orders = orderReadRepository.findBySymbol(symbol);
            log.info("Successfully fetched {} orders for symbol from read model: {}", orders.size(), symbol);
            response = getResponse("Orders fetched successfully", HttpStatus.OK, SUCCESS, orders);
            return response.toResponseEntity();
        } catch (Exception e) {
            log.error("Error fetching orders by symbol {} from read model: {}", symbol, e.getMessage(), e);
            response = getResponse("Error fetching orders: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, null);
            return response.toResponseEntity();
        }
    }

    /**
     * Get orders by status from read model (Redis)
     */
    public ResponseEntity<Response> getOrdersByStatus(String status) {
        log.info("Fetching orders by status from read model: {}", status);
        Response response;

        try {
            List<OrderReadModel> orders = orderReadRepository.findByStatus(status);
            log.info("Successfully fetched {} orders with status {} from read model", orders.size(), status);
            response = getResponse("Orders fetched successfully", HttpStatus.OK, SUCCESS, orders);
            return response.toResponseEntity();
        } catch (Exception e) {
            log.error("Error fetching orders by status {} from read model: {}", status, e.getMessage(), e);
            response = getResponse("Error fetching orders: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, null);
            return response.toResponseEntity();
        }
    }

    /**
     * Get order history with pagination and filters.
     * Queries PostgreSQL (write model) for historical data.
     * Supports filtering by userId, status, symbol, and date range.
     */
    public ResponseEntity<Response> getOrderHistory(Params params) {
        log.info("Fetching order history with params: userId={}, status={}, symbol={}, startDate={}, endDate={}, page={}, size={}",
                params.userId(), params.status(), params.searchValue(), params.startDate(), params.endDate(), params.page(), params.pageSize());
        Response response;

        try {
            // Parse status as String (for native query)
            String statusStr = null;
            if (params.status() != null && !params.status().trim().isEmpty()) {
                try {
                    // Validate status is a valid enum value
                    OrderStatus.valueOf(params.status().trim().toUpperCase());
                    statusStr = params.status().trim().toUpperCase();
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid order status: {}", params.status());
                    // Continue with null status (no filter)
                }
            }

            // Parse date range
            LocalDateTime startDate = null;
            LocalDateTime endDate = null;
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

            if (params.startDate() != null && !params.startDate().trim().isEmpty()) {
                try {
                    startDate = LocalDateTime.parse(params.startDate().trim(), formatter);
                } catch (Exception e) {
                    // Try alternative formats
                    try {
                        startDate = LocalDateTime.parse(params.startDate().trim(), DateTimeFormatter.ISO_LOCAL_DATE);
                    } catch (Exception ex) {
                        log.warn("Invalid start date format: {}", params.startDate());
                    }
                }
            }

            if (params.endDate() != null && !params.endDate().trim().isEmpty()) {
                try {
                    endDate = LocalDateTime.parse(params.endDate().trim(), formatter);
                } catch (Exception e) {
                    try {
                        endDate = LocalDateTime.parse(params.endDate().trim(), DateTimeFormatter.ISO_LOCAL_DATE).plusDays(1).minusSeconds(1);
                    } catch (Exception ex) {
                        log.warn("Invalid end date format: {}", params.endDate());
                    }
                }
            }

            // Parse symbol from searchValue if provided
            String symbol = null;
            if (params.searchValue() != null && !params.searchValue().trim().isEmpty()) {
                symbol = params.searchValue().trim();
            }

            // Build pageable with sorting
            Sort sort = Sort.by(Sort.Direction.DESC, "createdAt"); // Default: newest first
            Pageable pageable = PageRequest.of(
                    params.page() - 1, // Spring Data uses 0-based indexing
                    Math.min(params.pageSize(), 100), // Max 100 per page
                    sort
            );

            // Build JPA Specification for dynamic query
            Specification<Order> spec = buildOrderHistorySpecification(
                    params.userId(),
                    statusStr,
                    symbol,
                    startDate,
                    endDate
            );

            // Query PostgreSQL for order history using Specifications
            Page<Order> orderPage = orderRepository.findAll(spec, pageable);

            // Convert Order entities to OrderReadModel DTOs
            Page<OrderReadModel> readModelPage = orderPage.map(orderReadModelMapper::toReadModel);

            log.info("Successfully fetched {} orders (page {} of {}) from order history",
                    readModelPage.getNumberOfElements(), readModelPage.getNumber() + 1, readModelPage.getTotalPages());

            response = buildPageResponse(readModelPage, order -> order);
            return response.toResponseEntity();

        } catch (Exception e) {
            log.error("Error fetching order history: {}", e.getMessage(), e);
            response = getResponse("Error fetching order history: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, SERVER_ERROR, null);
            return response.toResponseEntity();
        }
    }

    /**
     * Builds JPA Specification for order history query with dynamic filters.
     * This approach properly handles null parameters in PostgreSQL.
     */
    private Specification<Order> buildOrderHistorySpecification(
            UUID userId,
            String status,
            String symbol,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by userId
            if (userId != null) {
                predicates.add(cb.equal(root.get("createdBy"), userId));
            }

            // Filter by status
            if (status != null && !status.trim().isEmpty()) {
                try {
                    OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                    predicates.add(cb.equal(root.get("status"), orderStatus));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid order status: {}", status);
                }
            }

            // Filter by symbol
            if (symbol != null && !symbol.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("symbol"), symbol));
            }

            // Filter by start date
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }

            // Filter by end date
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
