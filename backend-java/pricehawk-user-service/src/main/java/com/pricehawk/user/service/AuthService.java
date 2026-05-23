package com.pricehawk.user.service;

import com.pricehawk.common.exception.BusinessException;
import com.pricehawk.security.jwt.JwtProperties;
import com.pricehawk.security.jwt.JwtTokenProvider;
import com.pricehawk.user.domain.entity.AuthProvider;
import com.pricehawk.user.domain.entity.RefreshToken;
import com.pricehawk.user.domain.entity.Role;
import com.pricehawk.user.domain.entity.User;
import com.pricehawk.user.dto.request.LoginRequest;
import com.pricehawk.user.dto.request.RegisterRequest;
import com.pricehawk.user.dto.response.AuthResponse;
import com.pricehawk.user.dto.response.UserDTO;
import com.pricehawk.user.mapper.UserMapper;
import com.pricehawk.user.repository.RefreshTokenRepository;
import com.pricehawk.user.repository.RoleRepository;
import com.pricehawk.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration rejected: email already exists [{}]", request.getEmail());
            throw BusinessException.conflict("Email already registered");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> BusinessException.notFound("Role USER"));

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .authProvider(AuthProvider.LOCAL)
                .roles(Set.of(userRole))
                .build();

        user = userRepository.save(user);
        log.info("User registered: id={}, email={}", user.getId(), user.getEmail());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // findByEmailWithRoles: single JOIN FETCH — email + roles in one query
        User user = userRepository.findByEmailWithRoles(request.getEmail())
                .orElseGet(() -> {
                    log.warn("Login failed: email not found [{}]", request.getEmail());
                    throw BusinessException.unauthorized("Invalid credentials");
                });

        if (!user.isActive()) {
            log.warn("Login blocked: account disabled [userId={}]", user.getId());
            throw BusinessException.forbidden("Account is disabled");
        }

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: wrong password [userId={}]", user.getId());
            throw BusinessException.unauthorized("Invalid credentials");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("User logged in: id={}", user.getId());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(String rawToken) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> BusinessException.unauthorized("Refresh token not found"));

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(storedToken);
            log.warn("Refresh token expired: userId={}", storedToken.getUserId());
            throw BusinessException.unauthorized("Refresh token expired");
        }

        // Load with roles to generate correct claims in new access token
        User user = userRepository.findByIdWithRoles(storedToken.getUserId())
                .orElseThrow(() -> BusinessException.notFound("User"));

        List<String> roleNames = user.getRoles().stream().map(Role::getName).toList();
        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId().toString(), roleNames);

        UserDTO userDTO = userMapper.toDTO(user);
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(rawToken)
                .expiresIn(jwtProperties.getAccessExpiryMs() / 1000)
                .user(userDTO)
                .build();
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokenRepository.findByToken(rawToken).ifPresent(token -> {
            refreshTokenRepository.delete(token);
            log.info("User logged out: userId={}", token.getUserId());
        });
    }

    /**
     * Package-visible: shared bởi register, login, và OAuth2Service.
     * Caller phải đảm bảo user.roles đã được load (dùng findByIdWithRoles hoặc EAGER context).
     */
    AuthResponse buildAuthResponse(User user) {
        List<String> roleNames = user.getRoles().stream().map(Role::getName).toList();
        String accessToken  = jwtTokenProvider.generateAccessToken(
                user.getId().toString(), roleNames);
        String refreshValue = jwtTokenProvider.generateRefreshToken(user.getId().toString());

        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .token(refreshValue)
                .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshExpiryMs()))
                .build());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshValue)
                .expiresIn(jwtProperties.getAccessExpiryMs() / 1000)
                .user(userMapper.toDTO(user))
                .build();
    }
}
