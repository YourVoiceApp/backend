package com.love.yourvoiceback.user.dto;

import com.love.yourvoiceback.auth.domain.UserSocialAccount;

public record SocialAccountResponse(
        String provider,
        String email
) {
    public static SocialAccountResponse from(UserSocialAccount account) {
        return new SocialAccountResponse(
                account.getProvider().name(),
                account.getEmail()
        );
    }
}
