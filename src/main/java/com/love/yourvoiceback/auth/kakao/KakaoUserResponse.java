package com.love.yourvoiceback.auth.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserResponse(
        Long id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {
    public String providerUserId() {
        return id != null ? String.valueOf(id) : null;
    }

    public String email() {
        return kakaoAccount != null ? kakaoAccount.email() : null;
    }

    public boolean hasUsableEmail() {
        String email = email();
        return email != null
                && !email.isBlank()
                && kakaoAccount != null
                && Boolean.TRUE.equals(kakaoAccount.isEmailValid())
                && Boolean.TRUE.equals(kakaoAccount.isEmailVerified());
    }

    public record KakaoAccount(
            @JsonProperty("has_email") Boolean hasEmail,
            @JsonProperty("email_needs_agreement") Boolean emailNeedsAgreement,
            @JsonProperty("is_email_valid") Boolean isEmailValid,
            @JsonProperty("is_email_verified") Boolean isEmailVerified,
            String email
    ) {
    }
}
