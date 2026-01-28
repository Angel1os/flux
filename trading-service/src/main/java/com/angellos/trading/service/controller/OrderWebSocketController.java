package com.angellos.trading.service.controller;

import com.angellos.shared.utility.SecurityUtils;
import com.angellos.trading.service.domain.readmodel.OrderReadModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@Slf4j
public class OrderWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public OrderWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Send order update to specific user (authenticated)
     */
    public void sendOrderUpdateToUser(UUID userId, OrderReadModel order) {
        messagingTemplate.convertAndSend(
                "/topic/orders/user/" + userId,
                order
        );
    }

    /**
     * Broadcast to all (admin only)
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void broadcastOrderUpdate(OrderReadModel order) {
        messagingTemplate.convertAndSend("/topic/orders/all", order);
    }

    /**
     * Handle client subscription requests (optional)
     */
    @MessageMapping("/orders/subscribe")
    @PreAuthorize("hasAnyRole('TRADER', 'ADMIN', 'VIEWER')")
    public void subscribeToOrders(SimpMessageHeaderAccessor headerAccessor) {
        Authentication auth = (Authentication) headerAccessor.getUser();
        UUID userId = SecurityUtils.getCurrentUserId();

        log.info("User {} subscribed to order updates", userId);

        // Send confirmation
        messagingTemplate.convertAndSendToUser(
                auth.getName(),
                "/queue/orders/subscribed",
                "Subscribed to order updates"
        );
    }
}
