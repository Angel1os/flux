package com.angellos.portfolio.service.mapper;

import com.angellos.portfolio.service.domain.dto.TransactionDTO;
import com.angellos.portfolio.service.domain.model.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionDTO toDTO(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        return TransactionDTO.builder()
                .id(transaction.getId())
                .portfolioId(transaction.getPortfolio() != null ? transaction.getPortfolio().getId() : null)
                .orderId(transaction.getOrderId())
                .symbol(transaction.getSymbol())
                .type(transaction.getType())
                .quantity(transaction.getQuantity())
                .price(transaction.getPrice())
                .totalAmount(transaction.getTotalAmount())
                .fees(transaction.getFees())
                .status(transaction.getStatus())
                .timestamp(transaction.getTimestamp())
                .build();
    }
}
