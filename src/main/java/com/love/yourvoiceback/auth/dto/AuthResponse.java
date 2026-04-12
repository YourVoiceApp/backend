package com.love.yourvoiceback.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        String email
) {
    public static AuthResponse of(String accessToken, String refreshToken, Long userId, String email) {
        return new AuthResponse(accessToken, refreshToken, userId, email);
    }
}
