package com.pricehawk.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manages price-alert subscriptions in Redis.
 *
 * Schema:
 *   HASH  price_alert:{productId}   → { userId → targetPrice }
 *   STRING user_email:{userId}       → email address
 *
 * A targetPrice of "0" means the user wants any price drop alert (no floor).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceAlertSubscriptionService {

    private static final String ALERT_KEY_PREFIX = "price_alert:";
    private static final String EMAIL_KEY_PREFIX = "user_email:";

    private final StringRedisTemplate redisTemplate;

    /**
     * Register or update a subscription.
     * Stores email separately so it can be looked up without scanning the alert hash.
     */
    public void subscribe(String userId, String userEmail, String productId, BigDecimal targetPrice) {
        String alertKey = ALERT_KEY_PREFIX + productId;
        String priceStr = targetPrice != null ? targetPrice.toPlainString() : "0";

        redisTemplate.opsForHash().put(alertKey, userId, priceStr);

        if (userEmail != null && !userEmail.isBlank()) {
            redisTemplate.opsForValue().set(EMAIL_KEY_PREFIX + userId, userEmail);
        }

        log.info("Subscribed userId={} to productId={} at targetPrice={}", userId, productId, priceStr);
    }

    /**
     * Remove a user's subscription for a product.
     */
    public void unsubscribe(String userId, String productId) {
        String alertKey = ALERT_KEY_PREFIX + productId;
        redisTemplate.opsForHash().delete(alertKey, userId);
        log.info("Unsubscribed userId={} from productId={}", userId, productId);
    }

    /**
     * Return all subscribers whose targetPrice is >= newPrice (i.e., alert should fire).
     * targetPrice=0 means "any price drop" — always included.
     */
    public List<SubscriptionEntry> getTriggeredSubscribers(String productId, BigDecimal newPrice) {
        String alertKey = ALERT_KEY_PREFIX + productId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(alertKey);

        List<SubscriptionEntry> triggered = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String userId = (String) entry.getKey();
            String rawPrice = (String) entry.getValue();
            BigDecimal targetPrice = new BigDecimal(rawPrice);

            // Fire if: no floor (0) OR new price is at or below target
            if (targetPrice.compareTo(BigDecimal.ZERO) == 0 || newPrice.compareTo(targetPrice) <= 0) {
                triggered.add(new SubscriptionEntry(userId, targetPrice));
            }
        }
        return triggered;
    }

    /**
     * Return ALL subscribers for a product (used to broadcast product-level updates).
     */
    public List<SubscriptionEntry> getAllSubscribers(String productId) {
        String alertKey = ALERT_KEY_PREFIX + productId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(alertKey);

        List<SubscriptionEntry> result = new ArrayList<>(entries.size());
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            result.add(new SubscriptionEntry(
                    (String) entry.getKey(),
                    new BigDecimal((String) entry.getValue())
            ));
        }
        return result;
    }

    /**
     * Retrieve stored email for a user. Returns null if not found.
     */
    public String getUserEmail(String userId) {
        return redisTemplate.opsForValue().get(EMAIL_KEY_PREFIX + userId);
    }

    // -------------------------------------------------------------------------

    public record SubscriptionEntry(String userId, BigDecimal targetPrice) {}
}
