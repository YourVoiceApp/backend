package com.love.yourvoiceback.voice.dto.response;

import lombok.Builder;

@Builder
public record VoiceTextToSpeechResponse(
        Long speechRequestId,
        Long generatedAudioId,
        String streamUrl,
        String downloadUrl
) {
}
