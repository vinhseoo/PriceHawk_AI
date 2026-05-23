package com.pricehawk.notification.consumer;

import com.pricehawk.common.constants.MessageQueueConstants;
import com.pricehawk.common.event.AnalysisResultEvent;
import com.pricehawk.notification.dto.WsNotification;
import com.pricehawk.notification.service.PriceAlertSubscriptionService;
import com.pricehawk.notification.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Consumes analysis.completed events and fans out WebSocket notifications:
 *   1. Broadcast to all product-topic subscribers → /topic/product/{productId}
 *   2. Send private notification to each subscribed user   → /user/{userId}/queue/notifications
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisCompletedConsumer {

    private final WebSocketNotificationService wsService;
    private final PriceAlertSubscriptionService subscriptionService;

    @RabbitListener(queues = MessageQueueConstants.ANALYSIS_COMPLETED_QUEUE)
    public void onAnalysisCompleted(AnalysisResultEvent event) {
        log.info("Analysis completed: productId={} trustScore={}", event.getProductId(), event.getTrustScore());

        String productId = event.getProductId();

        // 1. Broadcast result to anyone watching this product's topic
        WsNotification broadcast = WsNotification.builder()
                .type("ANALYSIS_COMPLETE")
                .productId(productId)
                .message(buildBroadcastMessage(event))
                .data(event)
                .build();
        wsService.sendToProduct(productId, broadcast);

        // 2. Notify each subscribed user privately
        List<PriceAlertSubscriptionService.SubscriptionEntry> subscribers =
                subscriptionService.getAllSubscribers(productId);

        for (PriceAlertSubscriptionService.SubscriptionEntry sub : subscribers) {
            WsNotification personal = WsNotification.builder()
                    .type("ANALYSIS_COMPLETE")
                    .productId(productId)
                    .message(String.format("Phân tích sản phẩm hoàn tất. Điểm tin cậy: %.0f%%",
                            event.getTrustScore() != null ? event.getTrustScore() * 100 : 0))
                    .data(event)
                    .build();
            wsService.sendToUser(sub.userId(), personal);
        }
    }

    // -------------------------------------------------------------------------

    private String buildBroadcastMessage(AnalysisResultEvent event) {
        if (event.getTrustScore() == null) {
            return "Phân tích sản phẩm đã hoàn tất.";
        }
        double pct = event.getTrustScore() * 100;
        if (pct >= 80) {
            return String.format("Sản phẩm đáng tin cậy cao (%.0f%%). %s", pct,
                    event.getRecommendation() != null ? event.getRecommendation() : "");
        } else if (pct >= 50) {
            return String.format("Sản phẩm mức tin cậy trung bình (%.0f%%).", pct);
        } else {
            return String.format("Cảnh báo: Sản phẩm có dấu hiệu đáng ngờ (%.0f%%).", pct);
        }
    }
}
