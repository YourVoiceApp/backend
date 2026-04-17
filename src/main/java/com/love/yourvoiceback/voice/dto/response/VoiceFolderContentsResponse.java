package com.love.yourvoiceback.voice.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record VoiceFolderContentsResponse(
        long totalVoiceCount,
        List<VoiceFolderResponse> folders,
        List<OwnedVoiceAssetResponse> voices
) {
}
