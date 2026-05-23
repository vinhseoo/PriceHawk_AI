package com.pricehawk.notification.consumer;

import com.pricehawk.common.event.PriceUpdatedEvent;
import com.pricehawk.notification.dto.WsNotification;
import com.pricehawk.notification.service.EmailDigestService;
import com.pricehawk.notification.service.PriceAlertSubscriptionService;
import com.pricehawk.notification.service.WebSocketNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceUpdatedConsumerTest {

    @Mock WebSocketNotificationService wsService;
    @Mock PriceAlertSubscriptionService subscriptionService;
    @Mock EmailDigestService emailDigestService;

    @InjectMocks PriceUpdatedConsumer consumer;

    @Test
    void onPriceUpdated_alwaysBroadcastsTopic() {
        PriceUpdatedEvent event = buildEvent("prod123", new BigDecimal("180000"));
        when(subscriptionService.getTriggeredSubscribers(eq("prod123"), any())).thenReturn(List.of());

        consumer.onPriceUpdated(event);

        verify(wsService).sendToProduct(eq("prod123"), any(WsNotification.class));
    }

    @Test
    void onPriceUpdated_triggeredSubscribers_receiveAlertAndEmail() {
        PriceUpdatedEvent event = buildEvent("prod123", new BigDecimal("180000"));
        List<PriceAlertSubscriptionService.SubscriptionEntry> triggered = List.of(
                new PriceAlertSubscriptionService.SubscriptionEntry("user1", new BigDecimal("200000")),
                new PriceAlertSubscriptionService.SubscriptionEntry("user2", BigDecimal.ZERO)
        );
        when(subscriptionService.getTriggeredSubscribers(eq("prod123"), eq(new BigDecimal("180000"))))
                .thenReturn(triggered);

        consumer.onPriceUpdated(event);

        verify(wsService).sendToUser(eq("user1"), any(WsNotification.class));
        verify(wsService).sendToUser(eq("user2"), any(WsNotification.class));
        verify(emailDigestService).queuePriceAlert(eq("user1"), eq(event));
        verify(emailDigestService).queuePriceAlert(eq("user2"), eq(event));
    }

    @Test
    void onPriceUpdated_noTriggeredSubscribers_noPrivateAlerts() {
        PriceUpdatedEvent event = buildEvent("prod123", new BigDecimal("250000"));
        when(subscriptionService.getTriggeredSubscribers(any(), any())).thenReturn(List.of());

        consumer.onPriceUpdated(event);

        verify(wsService, never()).sendToUser(any(), any());
        verify(emailDigestService, never()).queuePriceAlert(any(), any());
    }

    @Test
    void onPriceUpdated_alertNotificationType_isPriceAlert() {
        PriceUpdatedEvent event = buildEvent("prod123", new BigDecimal("180000"));
        when(subscriptionService.getTriggeredSubscribers(any(), any())).thenReturn(
                List.of(new PriceAlertSubscriptionService.SubscriptionEntry("user1", new BigDecimal("200000")))
        );

        consumer.onPriceUpdated(event);

        ArgumentCaptor<WsNotification> captor = ArgumentCaptor.forClass(WsNotification.class);
        verify(wsService).sendToUser(eq("user1"), captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("PRICE_ALERT");
    }

    @Test
    void onPriceUpdated_broadcastNotificationType_isPriceUpdate() {
        PriceUpdatedEvent event = buildEvent("prod123", new BigDecimal("180000"));
        when(subscriptionService.getTriggeredSubscribers(any(), any())).thenReturn(List.of());

        consumer.onPriceUpdated(event);

        ArgumentCaptor<WsNotification> captor = ArgumentCaptor.forClass(WsNotification.class);
        verify(wsService).sendToProduct(eq("prod123"), captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("PRICE_UPDATE");
    }

    // -------------------------------------------------------------------------

    private PriceUpdatedEvent buildEvent(String productId, BigDecimal newPrice) {
        return PriceUpdatedEvent.builder()
                .productId(productId)
                .productName("Samsung Galaxy S24")
                .newPrice(newPrice)
                .previousPrice(new BigDecimal("220000"))
                .currency("VND")
                .build();
    }
}
