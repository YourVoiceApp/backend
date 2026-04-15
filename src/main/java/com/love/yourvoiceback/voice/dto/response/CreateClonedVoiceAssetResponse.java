package com.love.yourvoiceback.voice.dto.response;

import com.love.yourvoiceback.voice.domain.VoiceAsset;

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
