package com.pricehawk.user.service;

import com.pricehawk.common.exception.BusinessException;
import com.pricehawk.user.domain.entity.AuthProvider;
import com.pricehawk.user.domain.entity.Role;
import com.pricehawk.user.domain.entity.User;
import com.pricehawk.user.dto.response.AuthResponse;
import com.pricehawk.user.repository.RoleRepository;
import com.pricehawk.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2Service {

    private static final String GOOGLE_TOKENINFO_URL =
            "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthService authService;
    private final RestTemplate restTemplate;

    /**
     * Verify Google idToken BEFORE opening any DB transaction —
     * HTTP calls inside transactions hold the connection open unnecessarily.
     */
    public AuthResponse loginWithGoogle(String idToken) {
        Map<String, Object> tokenInfo = verifyGoogleToken(idToken);

        String email    = (String) tokenInfo.get("email");
        String googleSub = (String) tokenInfo.get("sub");
        String name     = (String) tokenInfo.get("name");
        String picture  = (String) tokenInfo.get("picture");

        if (email == null || googleSub == null) {
            throw BusinessException.badRequest("Invalid Google token: missing required claims");
        }

        return persistLogin(email, googleSub, name, picture);
    }

    @Transactional
    protected AuthResponse persistLogin(String email, String googleSub, String name, String picture) {
        // 1. First try to find by Google provider ID (most specific lookup)
        Optional<User> byProvider =
                userRepository.findByProviderIdAndAuthProvider(googleSub, AuthProvider.GOOGLE);

        if (byProvider.isPresent()) {
            User user = byProvider.get();
            log.info("Google login: existing user [{}] re-authenticated via Google", user.getId());
            return authService.buildAuthResponse(user);
        }

        // 2. Check if email already exists with a different provider
        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            if (existing.getAuthProvider() != AuthProvider.GOOGLE) {
                // Security: auto-linking would allow account takeover.
                // User must explicitly link accounts after local login.
                log.warn("Google login blocked: email [{}] already registered with provider [{}]",
                        email, existing.getAuthProvider());
                throw BusinessException.conflict(
                        "This email is already registered with a password. " +
                        "Please log in with your email and password.");
            }
            // Same email, same GOOGLE provider but providerId not stored yet (edge case)
            existing.setProviderId(googleSub);
            if (existing.getAvatarUrl() == null && picture != null) {
                existing.setAvatarUrl(picture);
            }
            userRepository.save(existing);
            log.info("Google login: linked providerId to existing Google user [{}]", existing.getId());
            return authService.buildAuthResponse(existing);
        }

        // 3. Brand-new user — create account
        User newUser = createGoogleUser(email, googleSub, name, picture);
        log.info("Google login: created new user [{}] via Google OAuth2", newUser.getId());
        return authService.buildAuthResponse(newUser);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> verifyGoogleToken(String idToken) {
        try {
            Map<String, Object> response = restTemplate.getForObject(
                    GOOGLE_TOKENINFO_URL + idToken, Map.class);
            if (response == null || response.containsKey("error_description")) {
                String error = response != null ? (String) response.get("error_description") : "null response";
                log.warn("Google tokeninfo rejected token: {}", error);
                throw BusinessException.unauthorized("Invalid Google token");
            }
            return response;
        } catch (RestClientException e) {
            log.error("Failed to reach Google tokeninfo endpoint: {}", e.getMessage());
            throw BusinessException.unauthorized("Could not verify Google token — please try again");
        }
    }

    private User createGoogleUser(String email, String sub, String name, String picture) {
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> BusinessException.notFound("Role USER"));

        return userRepository.save(User.builder()
                .email(email)
                .authProvider(AuthProvider.GOOGLE)
                .providerId(sub)
                .fullName(name)
                .avatarUrl(picture)
                .roles(Set.of(userRole))
                .build());
    }
}
