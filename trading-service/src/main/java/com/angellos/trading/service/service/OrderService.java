package com.angellos.trading.service.service;

import com.angellos.shared.dto.OrderRequest;
import com.angellos.shared.record.Response;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Write-side service for orders (CQRS Command side).
 * Handles all write operations: create, update, cancel.
 * 
 * Read operations are handled by OrderQueryService (CQRS Query side).
 */
public interface OrderService {

    ResponseEntity<Response> createOrder(OrderRequest orderRequest);

    ResponseEntity<Response> updateOrder(UUID id, OrderRequest orderRequest);

    ResponseEntity<Response> cancelOrder(UUID id);

    ResponseEntity<Response> executeOrder(UUID id, BigDecimal executionPrice, BigDecimal executionQuantity);
}
