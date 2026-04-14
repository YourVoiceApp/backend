package com.love.yourvoiceback.room.controller.dto.response;

import com.love.yourvoiceback.room.domain.RoomVoiceShare;
import com.love.yourvoiceback.room.enums.AccessScope;

import java.time.LocalDateTime;

public record RoomVoiceShareResponse(
        Long id,
        Long roomId,
        Long voiceAssetId,
        String voiceTitle,
        AccessScope accessScope,
        LocalDateTime sharedAt
) {
    public static RoomVoiceShareResponse from(RoomVoiceShare roomVoiceShare) {
        return new RoomVoiceShareResponse(
                roomVoiceShare.getId(),
                roomVoiceShare.getRoom().getId(),
                roomVoiceShare.getVoiceAsset().getId(),
                roomVoiceShare.getVoiceAsset().getTitle(),
                roomVoiceShare.getAccessScope(),
                roomVoiceShare.getSharedAt()
        );
    }
}
