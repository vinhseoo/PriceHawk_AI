package com.pricehawk.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * WebSocket message payload sent to connected clients.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WsNotification {

    /** ANALYSIS_COMPLETE | PRICE_ALERT | PRICE_UPDATE */
    private String type;

    private String productId;

    /** Human-readable message (Vietnamese). */
    private String message;

    /** Optional structured payload (analysis result, price info). */
    private Object data;

    @Builder.Default
    private Instant timestamp = Instant.now();
}
