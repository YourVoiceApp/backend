package com.love.yourvoiceback.room.controller.dto.request;

import com.love.yourvoiceback.room.enums.AccessScope;
import jakarta.validation.constraints.NotNull;

public record RoomVoiceShareUpdateRequest(
        @NotNull AccessScope accessScope
) {
}
