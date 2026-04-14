package com.love.yourvoiceback.room.controller.dto.request;

import com.love.yourvoiceback.room.enums.AccessScope;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RoomVoiceShareRequest(
        @NotEmpty List<Long> voiceAssetIds,
        @NotNull AccessScope accessScope
) {
}
