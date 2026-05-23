package com.pricehawk.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceAlertSubscriptionServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock HashOperations<String, Object, Object> hashOps;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks PriceAlertSubscriptionService service;

    @BeforeEach
    void setUp() {
        // lenient: not every test exercises both operations
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOps);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void subscribe_storesTargetPriceAndEmail() {
        service.subscribe("user1", "user1@test.com", "prod123", new BigDecimal("150000"));

        verify(hashOps).put("price_alert:prod123", "user1", "150000");
        verify(valueOps).set("user_email:user1", "user1@test.com");
    }

    @Test
    void subscribe_nullTargetPrice_storesZero() {
        service.subscribe("user1", "user1@test.com", "prod123", null);

        verify(hashOps).put("price_alert:prod123", "user1", "0");
    }

    @Test
    void unsubscribe_deletesHashEntry() {
        service.unsubscribe("user1", "prod123");

        verify(hashOps).delete("price_alert:prod123", "user1");
    }

    @Test
    void getTriggeredSubscribers_returnsUsersWhosePriceMet() {
        when(hashOps.entries("price_alert:prod123")).thenReturn(Map.of(
                "user1", "200000",   // target 200k, new price 180k → triggered
                "user2", "150000",   // target 150k, new price 180k → NOT triggered
                "user3", "0"         // any price → always triggered
        ));

        List<PriceAlertSubscriptionService.SubscriptionEntry> triggered =
                service.getTriggeredSubscribers("prod123", new BigDecimal("180000"));

        assertThat(triggered).extracting(PriceAlertSubscriptionService.SubscriptionEntry::userId)
                .containsExactlyInAnyOrder("user1", "user3");
    }

    @Test
    void getTriggeredSubscribers_exactTargetPrice_isTriggered() {
        when(hashOps.entries("price_alert:prod123")).thenReturn(Map.of(
                "user1", "180000"
        ));

        List<PriceAlertSubscriptionService.SubscriptionEntry> triggered =
                service.getTriggeredSubscribers("prod123", new BigDecimal("180000"));

        assertThat(triggered).hasSize(1);
    }

    @Test
    void getUserEmail_delegatesToRedis() {
        when(valueOps.get("user_email:user1")).thenReturn("user1@test.com");

        assertThat(service.getUserEmail("user1")).isEqualTo("user1@test.com");
    }

    @Test
    void getUserEmail_notFound_returnsNull() {
        when(valueOps.get("user_email:unknown")).thenReturn(null);

        assertThat(service.getUserEmail("unknown")).isNull();
    }
}
