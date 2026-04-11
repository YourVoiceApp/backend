package com.love.yourvoiceback.auth.google;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleIdTokenPayload(
        String sub,
        String aud,
        String email,
        @JsonProperty("email_verified") String emailVerified,
        String name
) {
    public boolean isEmailVerified() {
        return "true".equalsIgnoreCase(emailVerified);
    }
}
