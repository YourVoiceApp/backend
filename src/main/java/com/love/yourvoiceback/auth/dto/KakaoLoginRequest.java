package com.love.yourvoiceback.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(
        @NotBlank String code,
        String deviceInfo
) {
}
