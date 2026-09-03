package com.timecapsule.wishes.service;

import com.timecapsule.wishes.dto.request.LoginRequest;
import com.timecapsule.wishes.dto.request.RefreshTokenRequest;
import com.timecapsule.wishes.dto.request.RegisterRequest;
import com.timecapsule.wishes.dto.response.AuthResponse;
import com.timecapsule.wishes.dto.response.UserResponse;
import com.timecapsule.wishes.entity.User;
import com.timecapsule.wishes.exception.BusinessException;
import com.timecapsule.wishes.exception.ResourceNotFoundException;
import com.timecapsule.wishes.repository.UserRepository;
import com.timecapsule.wishes.security.JwtTokenProvider;
import com.timecapsule.wishes.security.UserPrincipal;
import com.timecapsule.wishes.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;
    private UUID sampleUserId;

    @BeforeEach
    void setUp() {
        sampleUserId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(sampleUserId)
                .email("john@example.com")
                .passwordHash("encoded_pass")
                .displayName("John Doe")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new user and return JWT tokens")
    void testRegister_Success() {
        RegisterRequest request = new RegisterRequest("john@example.com", "secret123", "John Doe");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtTokenProvider.generateAccessToken(sampleUserId, "john@example.com")).thenReturn("access-token-123");
        when(jwtTokenProvider.generateRefreshToken(sampleUserId, "john@example.com")).thenReturn("refresh-token-123");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access-token-123", response.accessToken());
        assertEquals("refresh-token-123", response.refreshToken());
        assertEquals(sampleUserId, response.userId());
        assertEquals("john@example.com", response.email());
        assertEquals("John Doe", response.displayName());
        assertEquals("Bearer", response.tokenType());

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw BusinessException with CONFLICT when email is already registered")
    void testRegister_EmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("john@example.com", "secret123", "John Doe");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.register(request));
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Email is already registered", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should successfully authenticate user and return tokens on login")
    void testLogin_Success() {
        LoginRequest request = new LoginRequest("john@example.com", "secret123");
        UserPrincipal principal = UserPrincipal.create(sampleUser);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateAccessToken(sampleUserId, "john@example.com")).thenReturn("access-token-123");
        when(jwtTokenProvider.generateRefreshToken(sampleUserId, "john@example.com")).thenReturn("refresh-token-123");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access-token-123", response.accessToken());
        assertEquals("refresh-token-123", response.refreshToken());
        assertEquals(sampleUserId, response.userId());
        assertEquals("john@example.com", response.email());
    }

    @Test
    @DisplayName("Should propagate BadCredentialsException when login credentials are invalid")
    void testLogin_BadCredentials() {
        LoginRequest request = new LoginRequest("john@example.com", "wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Should refresh token successfully when valid refresh token is provided")
    void testRefreshToken_Success() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        when(jwtTokenProvider.validateToken("valid-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken("valid-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.extractEmail("valid-refresh-token")).thenReturn("john@example.com");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));
        when(jwtTokenProvider.generateAccessToken(sampleUserId, "john@example.com")).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(sampleUserId, "john@example.com")).thenReturn("new-refresh-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new-access-token", response.accessToken());
        assertEquals("new-refresh-token", response.refreshToken());
        assertEquals(sampleUserId, response.userId());
    }

    @Test
    @DisplayName("Should throw UNAUTHORIZED when refresh token is invalid or expired")
    void testRefreshToken_InvalidToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-refresh-token");

        when(jwtTokenProvider.validateToken("invalid-refresh-token")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.refreshToken(request));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertEquals("Invalid or expired refresh token", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw UNAUTHORIZED when provided token is an access token instead of refresh token")
    void testRefreshToken_NotRefreshToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("access-token-used-as-refresh");

        when(jwtTokenProvider.validateToken("access-token-used-as-refresh")).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken("access-token-used-as-refresh")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.refreshToken(request));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    @DisplayName("Should retrieve current user profile successfully")
    void testGetCurrentUser_Success() {
        UserPrincipal principal = UserPrincipal.create(sampleUser);
        when(userRepository.findById(sampleUserId)).thenReturn(Optional.of(sampleUser));

        UserResponse response = authService.getCurrentUser(principal);

        assertNotNull(response);
        assertEquals(sampleUserId, response.id());
        assertEquals("john@example.com", response.email());
        assertEquals("John Doe", response.displayName());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user in principal is not in database")
    void testGetCurrentUser_NotFound() {
        UserPrincipal principal = UserPrincipal.create(sampleUser);
        when(userRepository.findById(sampleUserId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.getCurrentUser(principal));
    }
}
