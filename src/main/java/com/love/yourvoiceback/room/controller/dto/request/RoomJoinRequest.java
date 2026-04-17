package com.love.yourvoiceback.room.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RoomJoinRequest(
        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "inviteCode must be a 6-digit number")
        String inviteCode,
        String password
) {
}
