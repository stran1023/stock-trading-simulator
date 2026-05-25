package com.tradingapp.auth.service;

import com.tradingapp.auth.dto.AuthResponse;
import com.tradingapp.auth.dto.LoginRequest;
import com.tradingapp.auth.dto.RegisterRequest;
import com.tradingapp.auth.dto.RegisterResponse;
import com.tradingapp.auth.entity.RefreshToken;
import com.tradingapp.auth.entity.User;
import com.tradingapp.auth.repository.RefreshTokenRepository;
import com.tradingapp.auth.repository.UserRepository;
import com.tradingapp.auth.security.JwtService;
import com.tradingapp.auth.service.impl.AuthServiceImpl;
import com.tradingapp.common.exception.InvalidTokenException;
import com.tradingapp.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock JwtService jwtService;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshExpirationMs", 604800000L);
    }

    // ── register ───────────────────────────────────────────────────

    @Test
    void register_happyPath_returnsUserIdAndUsername() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.save(any())).thenReturn(savedUser(1L, "alice", "alice@example.com"));

        RegisterResponse res = authService.register(registerRequest("alice", "alice@example.com", "secret123"));

        assertThat(res.userId()).isEqualTo(1L);
        assertThat(res.username()).isEqualTo("alice");
        verify(passwordEncoder).encode("secret123");
    }

    @Test
    void register_emailAlreadyTaken_throwsIllegalArgument() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest("alice", "alice@example.com", "secret")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email");
    }

    @Test
    void register_usernameAlreadyTaken_throwsIllegalArgument() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest("alice", "alice@example.com", "secret")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username");
    }

    // ── login ──────────────────────────────────────────────────────

    @Test
    void login_happyPath_returnsTokens() {
        User user = savedUser(1L, "alice", "alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", user.getPassword())).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.getAccessExpirationMs()).thenReturn(900000L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse res = authService.login(loginRequest("alice@example.com", "secret123"));

        assertThat(res.accessToken()).isEqualTo("access-token");
        assertThat(res.expiresIn()).isEqualTo(900L);
        assertThat(res.refreshToken()).isNotBlank();
    }

    @Test
    void login_userNotFound_throws() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest("nobody@example.com", "secret")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void login_wrongPassword_throws() {
        User user = savedUser(1L, "alice", "alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest("alice@example.com", "wrong")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── refresh ────────────────────────────────────────────────────

    @Test
    void refresh_happyPath_rotatesToken() {
        User user = savedUser(1L, "alice", "alice@example.com");
        RefreshToken stored = validToken(user);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access");
        when(jwtService.getAccessExpirationMs()).thenReturn(900000L);

        AuthResponse res = authService.refresh("raw-token");

        assertThat(res.accessToken()).isEqualTo("new-access");
        assertThat(stored.isRevoked()).isTrue();
    }

    @Test
    void refresh_tokenNotFound_throws() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("bad-token"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void refresh_tokenRevoked_throws() {
        RefreshToken stored = validToken(savedUser(1L, "alice", "alice@example.com"));
        stored.setRevoked(true);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh("raw-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    void refresh_tokenExpired_throws() {
        RefreshToken stored = validToken(savedUser(1L, "alice", "alice@example.com"));
        stored.setExpiresAt(OffsetDateTime.now().minusDays(1));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh("raw-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    // ── logout ─────────────────────────────────────────────────────

    @Test
    void logout_revokesAllUserTokens() {
        User user = savedUser(1L, "alice", "alice@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        authService.logout(1L);

        verify(refreshTokenRepository).revokeAllByUser(user);
    }

    // ── helpers ────────────────────────────────────────────────────

    private RegisterRequest registerRequest(String username, String email, String password) {
        RegisterRequest r = new RegisterRequest();
        r.setUsername(username);
        r.setEmail(email);
        r.setPassword(password);
        return r;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest r = new LoginRequest();
        r.setEmail(email);
        r.setPassword(password);
        return r;
    }

    private User savedUser(Long id, String username, String email) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash("hashed-password");
        u.setBalance(new BigDecimal("1000000.0000"));
        return u;
    }

    private RefreshToken validToken(User user) {
        RefreshToken t = new RefreshToken();
        t.setUser(user);
        t.setRevoked(false);
        t.setExpiresAt(OffsetDateTime.now().plusDays(7));
        return t;
    }
}
