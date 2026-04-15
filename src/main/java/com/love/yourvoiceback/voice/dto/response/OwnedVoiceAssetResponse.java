package com.love.yourvoiceback.voice.dto.response;

import com.love.yourvoiceback.voice.domain.VoiceAsset;
import com.love.yourvoiceback.voice.domain.VoiceOwnership;

import java.time.LocalDateTime;

public record OwnedVoiceAssetResponse(
        String voiceKey,
        String externalVoiceId,
        String title,
        Long folderId,
        VoiceOwnership.AcquisitionType acquiredBy,
        LocalDateTime acquiredAt
) {
    public static OwnedVoiceAssetResponse from(VoiceOwnership voiceOwnership) {
        VoiceAsset voiceAsset = voiceOwnership.getVoiceAsset();
        return new OwnedVoiceAssetResponse(
                voiceAsset.getExternalVoiceId(),
                voiceAsset.getExternalVoiceId(),
                voiceAsset.getTitle(),
                voiceOwnership.getFolder() != null ? voiceOwnership.getFolder().getId() : null,
                voiceOwnership.getAcquiredBy(),
                voiceOwnership.getAcquiredAt()
        );
    }
}
