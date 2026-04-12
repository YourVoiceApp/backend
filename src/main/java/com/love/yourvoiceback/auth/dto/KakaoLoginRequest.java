package com.love.yourvoiceback.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(
        @NotBlank String accessToken,
        String deviceInfo
) {
}
