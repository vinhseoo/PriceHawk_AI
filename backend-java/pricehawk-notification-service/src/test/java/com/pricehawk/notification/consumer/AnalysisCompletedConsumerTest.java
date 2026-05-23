package com.pricehawk.notification.consumer;

import com.pricehawk.common.event.AnalysisResultEvent;
import com.pricehawk.notification.dto.WsNotification;
import com.pricehawk.notification.service.PriceAlertSubscriptionService;
import com.pricehawk.notification.service.WebSocketNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisCompletedConsumerTest {

    @Mock WebSocketNotificationService wsService;
    @Mock PriceAlertSubscriptionService subscriptionService;

    @InjectMocks AnalysisCompletedConsumer consumer;

    @Test
    void onAnalysisCompleted_broadcastsToProductTopic() {
        AnalysisResultEvent event = AnalysisResultEvent.builder()
                .productId("prod123")
                .trustScore(0.85)
                .build();
        when(subscriptionService.getAllSubscribers("prod123")).thenReturn(List.of());

        consumer.onAnalysisCompleted(event);

        ArgumentCaptor<WsNotification> captor = ArgumentCaptor.forClass(WsNotification.class);
        verify(wsService).sendToProduct(eq("prod123"), captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("ANALYSIS_COMPLETE");
    }

    @Test
    void onAnalysisCompleted_sendsPrivateNotificationToSubscribers() {
        AnalysisResultEvent event = AnalysisResultEvent.builder()
                .productId("prod123")
                .trustScore(0.90)
                .build();
        List<PriceAlertSubscriptionService.SubscriptionEntry> subs = List.of(
                new PriceAlertSubscriptionService.SubscriptionEntry("user1", null),
                new PriceAlertSubscriptionService.SubscriptionEntry("user2", null)
        );
        when(subscriptionService.getAllSubscribers("prod123")).thenReturn(subs);

        consumer.onAnalysisCompleted(event);

        verify(wsService, times(1)).sendToProduct(eq("prod123"), any());
        verify(wsService, times(1)).sendToUser(eq("user1"), any());
        verify(wsService, times(1)).sendToUser(eq("user2"), any());
    }

    @Test
    void onAnalysisCompleted_noSubscribers_onlyBroadcasts() {
        AnalysisResultEvent event = AnalysisResultEvent.builder()
                .productId("prod123")
                .trustScore(0.60)
                .build();
        when(subscriptionService.getAllSubscribers("prod123")).thenReturn(List.of());

        consumer.onAnalysisCompleted(event);

        verify(wsService, times(1)).sendToProduct(any(), any());
        verify(wsService, never()).sendToUser(any(), any());
    }

    @Test
    void onAnalysisCompleted_highTrustScore_broadcastMessageReflectsTrust() {
        AnalysisResultEvent event = AnalysisResultEvent.builder()
                .productId("prod123")
                .trustScore(0.92)
                .build();
        when(subscriptionService.getAllSubscribers("prod123")).thenReturn(List.of());

        consumer.onAnalysisCompleted(event);

        ArgumentCaptor<WsNotification> captor = ArgumentCaptor.forClass(WsNotification.class);
        verify(wsService).sendToProduct(any(), captor.capture());
        assertThat(captor.getValue().getMessage()).contains("đáng tin cậy cao");
    }

    @Test
    void onAnalysisCompleted_lowTrustScore_broadcastWarning() {
        AnalysisResultEvent event = AnalysisResultEvent.builder()
                .productId("prod123")
                .trustScore(0.30)
                .build();
        when(subscriptionService.getAllSubscribers("prod123")).thenReturn(List.of());

        consumer.onAnalysisCompleted(event);

        ArgumentCaptor<WsNotification> captor = ArgumentCaptor.forClass(WsNotification.class);
        verify(wsService).sendToProduct(any(), captor.capture());
        assertThat(captor.getValue().getMessage()).contains("Cảnh báo");
    }
}
