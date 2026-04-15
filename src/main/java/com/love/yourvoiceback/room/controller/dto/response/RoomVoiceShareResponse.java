package com.love.yourvoiceback.room.controller.dto.response;

import com.love.yourvoiceback.room.domain.RoomVoiceShare;
import com.love.yourvoiceback.room.enums.AccessScope;

import java.time.LocalDateTime;

public record RoomVoiceShareResponse(
        Long id,
        Long roomId,
        String voiceKey,
        String externalVoiceId,
        String voiceTitle,
        AccessScope accessScope,
        LocalDateTime sharedAt
) {
    public static RoomVoiceShareResponse from(RoomVoiceShare roomVoiceShare) {
        return new RoomVoiceShareResponse(
                roomVoiceShare.getId(),
                roomVoiceShare.getRoom().getId(),
                roomVoiceShare.getVoiceAsset().getExternalVoiceId(),
                roomVoiceShare.getVoiceAsset().getExternalVoiceId(),
                roomVoiceShare.getVoiceAsset().getTitle(),
                roomVoiceShare.getAccessScope(),
                roomVoiceShare.getSharedAt()
        );
    }
}
