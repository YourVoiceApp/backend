package com.love.yourvoiceback.voice.dto;

import com.love.yourvoiceback.voice.VoiceAsset;

public record CreateClonedVoiceAssetResponse(
        Long voiceAssetId,
        String externalVoiceId,
        String title,
        VoiceAsset.VoiceAssetStatus status
) {
    public static CreateClonedVoiceAssetResponse from(VoiceAsset voiceAsset) {
        return new CreateClonedVoiceAssetResponse(
                voiceAsset.getId(),
                voiceAsset.getExternalVoiceId(),
                voiceAsset.getTitle(),
                voiceAsset.getStatus()
        );
    }
}
