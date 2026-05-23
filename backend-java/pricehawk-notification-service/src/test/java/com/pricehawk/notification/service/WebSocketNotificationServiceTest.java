package com.pricehawk.notification.service;

import com.pricehawk.notification.dto.WsNotification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketNotificationServiceTest {

    @Mock SimpMessagingTemplate messagingTemplate;

    @InjectMocks WebSocketNotificationService service;

    @Test
    void sendToUser_invokesConvertAndSendToUser() {
        WsNotification notification = WsNotification.builder()
                .type("PRICE_ALERT")
                .productId("prod123")
                .message("Giá mục tiêu đạt!")
                .build();

        service.sendToUser("user1", notification);

        verify(messagingTemplate).convertAndSendToUser(
                eq("user1"),
                eq("/queue/notifications"),
                eq(notification)
        );
    }

    @Test
    void sendToProduct_invokesConvertAndSend_withCorrectTopic() {
        WsNotification notification = WsNotification.builder()
                .type("ANALYSIS_COMPLETE")
                .productId("prod456")
                .build();

        service.sendToProduct("prod456", notification);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/product/prod456"),
                eq(notification)
        );
    }

    @Test
    void sendToProduct_usesProductIdInPath() {
        WsNotification notification = WsNotification.builder().type("PRICE_UPDATE").build();

        service.sendToProduct("abc-123", notification);

        ArgumentCaptor<String> destCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate).convertAndSend(destCaptor.capture(), eq(notification));
        assertThat(destCaptor.getValue()).isEqualTo("/topic/product/abc-123");
    }
}
