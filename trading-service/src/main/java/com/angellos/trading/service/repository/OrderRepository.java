package com.angellos.trading.service.repository;

import com.angellos.shared.enums.OrderStatus;
import com.angellos.trading.service.domain.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    List<Order> findOrdersByCreatedBy(UUID createdBy);

    List<Order> findOrdersByStatus(OrderStatus status);

    List<Order> findOrdersBySymbol(String symbol);

}
