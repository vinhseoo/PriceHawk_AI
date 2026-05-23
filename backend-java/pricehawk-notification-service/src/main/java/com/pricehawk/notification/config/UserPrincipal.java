package com.pricehawk.notification.config;

import java.security.Principal;

/**
 * Lightweight Principal backed by the X-User-Id header injected by the API Gateway.
 * No JWT validation needed here — Gateway already validated the token upstream.
 */
public record UserPrincipal(String name) implements Principal {
    @Override
    public String getName() {
        return name;
    }
}
