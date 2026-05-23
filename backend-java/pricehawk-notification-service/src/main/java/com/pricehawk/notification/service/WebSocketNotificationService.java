package com.pricehawk.notification.service;

import com.pricehawk.notification.dto.WsNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Sends WebSocket messages via STOMP.
 *
 * Routing:
 *   sendToUser   → /user/{userId}/queue/notifications  (private, user-specific)
 *   sendToProduct → /topic/product/{productId}         (broadcast, anyone subscribed)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send a private notification to a specific user.
     * Requires the user to be connected with a valid X-User-Id principal.
     */
    public void sendToUser(String userId, WsNotification notification) {
        messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", notification);
        log.debug("WS → user={} type={}", userId, notification.getType());
    }

    /**
     * Broadcast a product update to all subscribers of that product's topic.
     * Used for analysis results and general price changes.
     */
    public void sendToProduct(String productId, WsNotification notification) {
        messagingTemplate.convertAndSend("/topic/product/" + productId, notification);
        log.debug("WS → /topic/product/{} type={}", productId, notification.getType());
    }
}
