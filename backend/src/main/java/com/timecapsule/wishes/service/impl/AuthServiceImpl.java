package com.timecapsule.wishes.service.impl;

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
import com.timecapsule.wishes.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email is already registered", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName().trim())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with id: {}", savedUser.getId());

        String accessToken = jwtTokenProvider.generateAccessToken(savedUser.getId(), savedUser.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.getId(), savedUser.getEmail());

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getExpirationMs(),
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getDisplayName()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        log.info("User authenticated successfully with id: {}", principal.getId());

        String accessToken = jwtTokenProvider.generateAccessToken(principal.getId(), principal.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(principal.getId(), principal.getEmail());

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getExpirationMs(),
                principal.getId(),
                principal.getEmail(),
                principal.getDisplayName()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.refreshToken();

        if (!jwtTokenProvider.validateToken(token) || !jwtTokenProvider.isRefreshToken(token)) {
            throw new BusinessException("Invalid or expired refresh token", HttpStatus.UNAUTHORIZED);
        }

        String email = jwtTokenProvider.extractEmail(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.UNAUTHORIZED));

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail());

        return AuthResponse.of(
                newAccessToken,
                newRefreshToken,
                jwtTokenProvider.getExpirationMs(),
                user.getId(),
                user.getEmail(),
                user.getDisplayName()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getCreatedAt());
    }
}
