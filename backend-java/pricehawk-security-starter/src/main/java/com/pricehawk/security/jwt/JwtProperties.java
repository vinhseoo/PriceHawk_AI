package com.pricehawk.security.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "pricehawk.jwt")
public class JwtProperties {
    private String secret = "default-secret-change-in-production";
    private long accessExpiryMs = 900000L;    // 15 minutes
    private long refreshExpiryMs = 604800000L; // 7 days
}
