package com.timecapsule.wishes.dto.response;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UUID userId,
        String email,
        String displayName
) {
    public static AuthResponse of(
            String accessToken,
            String refreshToken,
            long expiresIn,
            UUID userId,
            String email,
            String displayName
    ) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, userId, email, displayName);
    }
}
