package com.love.yourvoiceback.user.dto;

import com.love.yourvoiceback.user.User;

public record MeResponse(
        Long id,
        String nickName,
        String email,
        boolean hasPassword
) {
    public static MeResponse from(User user) {
        return new MeResponse(
                user.getId(),
                user.getNickName(),
                user.getEmail(),
                user.getPassword() != null && !user.getPassword().isBlank()
        );
    }
}
