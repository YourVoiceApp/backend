package com.love.yourvoiceback.voice.dto.response;

import com.love.yourvoiceback.voice.domain.VoiceAsset;

public record CreateClonedVoiceAssetResponse(
        String voiceKey,
        String externalVoiceId,
        String title
) {
    public static CreateClonedVoiceAssetResponse from(VoiceAsset voiceAsset) {
        return new CreateClonedVoiceAssetResponse(
                voiceAsset.getExternalVoiceId(),
                voiceAsset.getExternalVoiceId(),
                voiceAsset.getTitle()
        );
    }
}
