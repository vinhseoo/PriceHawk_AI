package com.pricehawk.user.auth;

import com.pricehawk.common.exception.BusinessException;
import com.pricehawk.security.jwt.JwtProperties;
import com.pricehawk.security.jwt.JwtTokenProvider;
import com.pricehawk.user.domain.entity.AuthProvider;
import com.pricehawk.user.domain.entity.RefreshToken;
import com.pricehawk.user.domain.entity.Role;
import com.pricehawk.user.domain.entity.SubscriptionPlan;
import com.pricehawk.user.domain.entity.User;
import com.pricehawk.user.dto.request.LoginRequest;
import com.pricehawk.user.dto.request.RegisterRequest;
import com.pricehawk.user.dto.response.AuthResponse;
import com.pricehawk.user.dto.response.UserDTO;
import com.pricehawk.user.mapper.UserMapper;
import com.pricehawk.user.repository.RefreshTokenRepository;
import com.pricehawk.user.repository.RoleRepository;
import com.pricehawk.user.repository.UserRepository;
import com.pricehawk.user.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock JwtProperties jwtProperties;
    @Mock UserMapper userMapper;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, roleRepository, refreshTokenRepository,
                passwordEncoder, jwtTokenProvider, jwtProperties, userMapper);
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_newEmail_returnsTokens() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@test.com");
        req.setPassword("password123");
        req.setFullName("Test User");

        Role role = Role.builder().id(UUID.randomUUID()).name("USER").build();
        User savedUser = User.builder()
                .email("new@test.com")
                .fullName("Test User")
                .authProvider(AuthProvider.LOCAL)
                .subscriptionPlan(SubscriptionPlan.FREE)
                .roles(Set.of(role))
                .build();
        savedUser.setId(UUID.randomUUID()); // simulate DB-generated id

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateAccessToken(any(), any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh-token");
        when(jwtProperties.getAccessExpiryMs()).thenReturn(900_000L);
        when(jwtProperties.getRefreshExpiryMs()).thenReturn(604_800_000L);
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userMapper.toDTO(any(User.class))).thenReturn(
                UserDTO.builder().email("new@test.com").build());

        AuthResponse response = authService.register(req);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        verify(userRepository).save(any(User.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("existing@test.com");
        req.setPassword("password123");
        req.setFullName("Test User");

        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already registered");
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_correctCredentials_returnsTokens() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@test.com");
        req.setPassword("correct");

        Role role = Role.builder().id(UUID.randomUUID()).name("USER").build();
        User user = User.builder()
                .email("user@test.com")
                .passwordHash("hashed-correct")
                .roles(Set.of(role))
                .build();
        user.setActive(true);
        user.setId(UUID.randomUUID()); // simulate DB-generated id

        when(userRepository.findByEmailWithRoles("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct", "hashed-correct")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtTokenProvider.generateAccessToken(any(), any())).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh");
        when(jwtProperties.getAccessExpiryMs()).thenReturn(900_000L);
        when(jwtProperties.getRefreshExpiryMs()).thenReturn(604_800_000L);
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userMapper.toDTO(any(User.class))).thenReturn(UserDTO.builder().build());

        AuthResponse response = authService.login(req);

        assertThat(response.getAccessToken()).isEqualTo("access");
        verify(userRepository).save(user); // lastLoginAt updated
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@test.com");
        req.setPassword("wrong");

        User user = User.builder()
                .email("user@test.com")
                .passwordHash("hashed")
                .roles(Set.of())
                .build();
        user.setActive(true);

        when(userRepository.findByEmailWithRoles("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void login_unknownEmail_throwsUnauthorized() {
        LoginRequest req = new LoginRequest();
        req.setEmail("unknown@test.com");
        req.setPassword("password");

        when(userRepository.findByEmailWithRoles("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void login_disabledAccount_throwsForbidden() {
        LoginRequest req = new LoginRequest();
        req.setEmail("disabled@test.com");
        req.setPassword("password");

        User user = User.builder()
                .email("disabled@test.com")
                .passwordHash("hash")
                .roles(Set.of())
                .build();
        user.setActive(false);

        when(userRepository.findByEmailWithRoles("disabled@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("disabled");
    }

    // ── refresh token ─────────────────────────────────────────────────────────

    @Test
    void refreshToken_expiredToken_throwsUnauthorized() {
        RefreshToken expired = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .token("expired-token")
                .expiresAt(Instant.now().minusSeconds(1))
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refreshToken("expired-token"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepository).delete(expired);
    }

    @Test
    void refreshToken_unknownToken_throwsUnauthorized() {
        when(refreshTokenRepository.findByToken("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken("ghost"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not found");
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    void logout_validToken_deletesRefreshToken() {
        RefreshToken token = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .token("my-token")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenRepository.findByToken("my-token")).thenReturn(Optional.of(token));

        authService.logout("my-token");

        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void logout_unknownToken_noException() {
        when(refreshTokenRepository.findByToken("ghost")).thenReturn(Optional.empty());

        authService.logout("ghost"); // should not throw
    }
}
