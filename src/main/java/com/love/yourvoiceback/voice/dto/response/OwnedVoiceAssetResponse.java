package com.love.yourvoiceback.voice.dto.response;

import com.love.yourvoiceback.voice.domain.VoiceAsset;
import com.love.yourvoiceback.voice.domain.VoiceOwnership;

import java.time.LocalDateTime;

public record OwnedVoiceAssetResponse(
        Long voiceAssetId,
        String externalVoiceId,
        String title,
        VoiceAsset.VoiceAssetStatus status,
        String sampleAudioUrl,
        Long folderId,
        VoiceOwnership.AcquisitionType acquiredBy,
        LocalDateTime acquiredAt
) {
    public static OwnedVoiceAssetResponse from(VoiceOwnership voiceOwnership) {
        VoiceAsset voiceAsset = voiceOwnership.getVoiceAsset();
        return new OwnedVoiceAssetResponse(
                voiceAsset.getId(),
                voiceAsset.getExternalVoiceId(),
                voiceAsset.getTitle(),
                voiceAsset.getStatus(),
                voiceAsset.getSampleAudioUrl(),
                voiceOwnership.getFolder() != null ? voiceOwnership.getFolder().getId() : null,
                voiceOwnership.getAcquiredBy(),
                voiceOwnership.getAcquiredAt()
        );
    }
}
