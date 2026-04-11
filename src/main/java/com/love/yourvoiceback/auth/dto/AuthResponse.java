package com.love.yourvoiceback.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        String email
) {
}
