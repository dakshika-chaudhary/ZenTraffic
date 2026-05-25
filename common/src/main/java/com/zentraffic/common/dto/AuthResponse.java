package com.zentraffic.common.dto;

public record AuthResponse(
        String token,
        String refreshToken,
        Long expiresInSeconds,
        Long refreshExpiresInSeconds,
        UserProfile profile
) {
}
