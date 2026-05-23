package com.pricehawk.notification.consumer;

import com.pricehawk.common.constants.MessageQueueConstants;
import com.pricehawk.common.event.PriceUpdatedEvent;
import com.pricehawk.notification.dto.WsNotification;
import com.pricehawk.notification.service.EmailDigestService;
import com.pricehawk.notification.service.PriceAlertSubscriptionService;
import com.pricehawk.notification.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Consumes price.updated events.
 *
 * For each event:
 *   1. Broadcast the price change to all product-topic subscribers.
 *   2. For subscribers whose targetPrice threshold is crossed, send a private
 *      WebSocket PRICE_ALERT and queue an email digest entry.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceUpdatedConsumer {

    private final WebSocketNotificationService wsService;
    private final PriceAlertSubscriptionService subscriptionService;
    private final EmailDigestService emailDigestService;

    @RabbitListener(queues = MessageQueueConstants.PRICE_UPDATED_QUEUE)
    public void onPriceUpdated(PriceUpdatedEvent event) {
        log.info("Price updated: productId={} newPrice={}", event.getProductId(), event.getNewPrice());

        String productId = event.getProductId();
        BigDecimal newPrice = event.getNewPrice();

        // 1. Broadcast the price update to all product-topic subscribers
        WsNotification broadcastMsg = WsNotification.builder()
                .type("PRICE_UPDATE")
                .productId(productId)
                .message(buildPriceUpdateMessage(event))
                .data(event)
                .build();
        wsService.sendToProduct(productId, broadcastMsg);

        // 2. Find subscribers whose alert threshold has been triggered
        List<PriceAlertSubscriptionService.SubscriptionEntry> triggered =
                subscriptionService.getTriggeredSubscribers(productId, newPrice);

        for (PriceAlertSubscriptionService.SubscriptionEntry sub : triggered) {
            sendPriceAlert(sub.userId(), event);
            emailDigestService.queuePriceAlert(sub.userId(), event);
        }

        if (!triggered.isEmpty()) {
            log.info("Price alert fired for {} subscribers on productId={}", triggered.size(), productId);
        }
    }

    // -------------------------------------------------------------------------

    private void sendPriceAlert(String userId, PriceUpdatedEvent event) {
        String message = buildAlertMessage(event);
        WsNotification alert = WsNotification.builder()
                .type("PRICE_ALERT")
                .productId(event.getProductId())
                .message(message)
                .data(event)
                .build();
        wsService.sendToUser(userId, alert);
    }

    private String buildPriceUpdateMessage(PriceUpdatedEvent event) {
        if (event.getPreviousPrice() == null) {
            return String.format("Giá %s cập nhật: %s %s",
                    event.getProductName() != null ? event.getProductName() : "sản phẩm",
                    event.getNewPrice().toPlainString(), event.getCurrency());
        }
        boolean decreased = event.getNewPrice().compareTo(event.getPreviousPrice()) < 0;
        return String.format("Giá %s %s: %s → %s %s",
                event.getProductName() != null ? event.getProductName() : "sản phẩm",
                decreased ? "giảm" : "tăng",
                event.getPreviousPrice().toPlainString(),
                event.getNewPrice().toPlainString(),
                event.getCurrency());
    }

    private String buildAlertMessage(PriceUpdatedEvent event) {
        return String.format("Giá mục tiêu đạt! %s hiện có giá %s %s",
                event.getProductName() != null ? event.getProductName() : event.getProductId(),
                event.getNewPrice().toPlainString(),
                event.getCurrency());
    }
}
