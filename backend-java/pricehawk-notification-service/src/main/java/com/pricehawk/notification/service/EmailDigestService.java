package com.pricehawk.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricehawk.common.event.PriceUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Queues per-user price-alert emails in Redis and sends a weekly digest every Monday at 09:00.
 *
 * Schema:
 *   LIST email_pending:{userId} → JSON-serialised PriceUpdatedEvent payloads
 *
 * The JavaMailSender may be null when SMTP is not configured — digest is silently skipped.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailDigestService {

    private static final String PENDING_KEY_PREFIX = "email_pending:";

    private final StringRedisTemplate redisTemplate;
    private final PriceAlertSubscriptionService subscriptionService;
    private final ObjectMapper objectMapper;

    @Value("${pricehawk.notifications.from-email:noreply@pricehawk.vn}")
    private String fromEmail;

    /** Null when spring.mail is not configured — gracefully disabled. */
    @Autowired(required = false)
    private JavaMailSender mailSender;

    /**
     * Append a price-drop event to the user's pending digest queue.
     * Called by PriceUpdatedConsumer when an alert threshold is crossed.
     */
    public void queuePriceAlert(String userId, PriceUpdatedEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.opsForList().rightPush(PENDING_KEY_PREFIX + userId, json);
            log.debug("Queued price alert for userId={} product={}", userId, event.getProductId());
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialise PriceUpdatedEvent for userId={}: {}", userId, e.getMessage());
        }
    }

    /**
     * Weekly digest — every Monday at 09:00 server time.
     * Sends one email per user that has pending alerts, then deletes the list.
     */
    @Scheduled(cron = "0 0 9 * * MON")
    public void sendWeeklyDigest() {
        if (mailSender == null) {
            log.debug("Mail sender not configured — skipping weekly digest");
            return;
        }

        Set<String> keys = redisTemplate.keys(PENDING_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            log.debug("No pending email alerts — digest skipped");
            return;
        }

        log.info("Sending weekly digest to {} users", keys.size());
        for (String key : keys) {
            String userId = key.substring(PENDING_KEY_PREFIX.length());
            processUserDigest(userId, key);
        }
    }

    // -------------------------------------------------------------------------

    private void processUserDigest(String userId, String redisKey) {
        String email = subscriptionService.getUserEmail(userId);
        if (email == null || email.isBlank()) {
            log.warn("No email found for userId={} — removing stale digest queue", userId);
            redisTemplate.delete(redisKey);
            return;
        }

        List<String> rawItems = redisTemplate.opsForList().range(redisKey, 0, -1);
        if (rawItems == null || rawItems.isEmpty()) {
            redisTemplate.delete(redisKey);
            return;
        }

        try {
            String body = buildDigestBody(rawItems);
            sendDigestEmail(email, body, rawItems.size());
            redisTemplate.delete(redisKey);
            log.info("Weekly digest sent to {} ({} alerts)", email, rawItems.size());
        } catch (Exception e) {
            log.error("Failed to send digest to userId={} email={}: {}", userId, email, e.getMessage(), e);
            // Leave the queue intact — will retry next week
        }
    }

    private String buildDigestBody(List<String> rawItems) {
        StringBuilder sb = new StringBuilder();
        sb.append("Chào bạn,\n\n");
        sb.append("PriceHawk đã phát hiện các cập nhật giá bạn quan tâm trong tuần này:\n\n");

        for (String json : rawItems) {
            try {
                PriceUpdatedEvent event = objectMapper.readValue(json, PriceUpdatedEvent.class);
                sb.append(String.format("• %s\n", event.getProductName() != null ? event.getProductName() : event.getProductId()));
                if (event.getPreviousPrice() != null) {
                    sb.append(String.format("  Giá cũ: %s %s → Giá mới: %s %s\n",
                            event.getPreviousPrice().toPlainString(), event.getCurrency(),
                            event.getNewPrice().toPlainString(), event.getCurrency()));
                } else {
                    sb.append(String.format("  Giá mới: %s %s\n",
                            event.getNewPrice().toPlainString(), event.getCurrency()));
                }
                sb.append("\n");
            } catch (JsonProcessingException e) {
                log.warn("Skipping malformed digest item: {}", e.getMessage());
            }
        }

        sb.append("Xem chi tiết tại: https://pricehawk.vn\n\n");
        sb.append("Trân trọng,\nĐội ngũ PriceHawk");
        return sb.toString();
    }

    private void sendDigestEmail(String toEmail, String body, int alertCount) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(String.format("PriceHawk: %d cập nhật giá trong tuần này", alertCount));
        message.setText(body);
        mailSender.send(message);
    }
}
