package com.pricehawk.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pricehawk.user.domain.entity.AuthProvider;
import com.pricehawk.user.domain.entity.SubscriptionPlan;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO {

    private UUID id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private AuthProvider authProvider;
    private SubscriptionPlan subscriptionPlan;
    private int dailySearchCount;
    private int dailySearchRemaining;
    private Set<String> roles;
    private Map<String, Object> preferences;
    private Instant lastLoginAt;
    private Instant createdAt;
}
