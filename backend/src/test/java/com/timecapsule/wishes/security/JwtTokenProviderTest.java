package com.timecapsule.wishes.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secret = "test-secret-key-that-is-at-least-256-bits-long-for-testing-purposes-123456";
    private final long expirationMs = 3600000; // 1 hour
    private final long refreshExpirationMs = 86400000; // 24 hours

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secret, expirationMs, refreshExpirationMs);
    }

    @Test
    @DisplayName("Should generate valid access token and extract subject and userId")
    void testGenerateAndValidateAccessToken() {
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";

        String token = jwtTokenProvider.generateAccessToken(userId, email);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertTrue(jwtTokenProvider.isAccessToken(token));
        assertFalse(jwtTokenProvider.isRefreshToken(token));
        assertEquals(email, jwtTokenProvider.extractEmail(token));
        assertEquals(userId, jwtTokenProvider.extractUserId(token));
    }

    @Test
    @DisplayName("Should generate valid refresh token")
    void testGenerateAndValidateRefreshToken() {
        UUID userId = UUID.randomUUID();
        String email = "refresh@example.com";

        String token = jwtTokenProvider.generateRefreshToken(userId, email);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertTrue(jwtTokenProvider.isRefreshToken(token));
        assertFalse(jwtTokenProvider.isAccessToken(token));
        assertEquals(email, jwtTokenProvider.extractEmail(token));
        assertEquals(userId, jwtTokenProvider.extractUserId(token));
    }

    @Test
    @DisplayName("Should return false for invalid or tampered token")
    void testInvalidToken() {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.token";
        assertFalse(jwtTokenProvider.validateToken(invalidToken));
    }

    @Test
    @DisplayName("Should return false for expired token")
    void testExpiredToken() {
        // Create a provider with 0ms expiration
        JwtTokenProvider expiredProvider = new JwtTokenProvider(secret, -1000, -1000);
        String token = expiredProvider.generateAccessToken(UUID.randomUUID(), "expired@example.com");

        assertFalse(jwtTokenProvider.validateToken(token));
    }
}
