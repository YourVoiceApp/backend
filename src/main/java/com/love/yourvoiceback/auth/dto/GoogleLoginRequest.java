package com.love.yourvoiceback.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank String idToken,
        String deviceInfo
) {
}
