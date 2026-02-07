package com.angellos.portfolio.service.controller;

import com.angellos.portfolio.service.domain.dto.PortfolioDTO;
import com.angellos.portfolio.service.domain.model.Portfolio;
import com.angellos.portfolio.service.mapper.PortfolioMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * WebSocket controller for real-time portfolio updates.
 * Sends portfolio updates to subscribed clients via STOMP.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class PortfolioWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final PortfolioMapper portfolioMapper;

    /**
     * Sends portfolio update to a specific user.
     * 
     * @param portfolioId Portfolio ID
     * @param portfolio Portfolio entity
     */
    public void sendPortfolioUpdate(UUID portfolioId, Portfolio portfolio) {
        try {
            PortfolioDTO dto = portfolioMapper.toDTO(portfolio);
            String destination = "/topic/portfolios/" + portfolioId;
            
            messagingTemplate.convertAndSend(destination, dto);
            log.debug("Sent portfolio update to destination: {}, portfolioId: {}", destination, portfolioId);
        } catch (Exception e) {
            log.error("Error sending portfolio update via WebSocket: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcasts portfolio update to all subscribers of a portfolio.
     * 
     * @param portfolioId Portfolio ID
     * @param portfolioDTO Portfolio DTO
     */
    public void broadcastPortfolioUpdate(UUID portfolioId, PortfolioDTO portfolioDTO) {
        try {
            String destination = "/topic/portfolios/" + portfolioId;
            messagingTemplate.convertAndSend(destination, portfolioDTO);
            log.debug("Broadcasted portfolio update to destination: {}, portfolioId: {}", destination, portfolioId);
        } catch (Exception e) {
            log.error("Error broadcasting portfolio update via WebSocket: {}", e.getMessage(), e);
        }
    }
}
