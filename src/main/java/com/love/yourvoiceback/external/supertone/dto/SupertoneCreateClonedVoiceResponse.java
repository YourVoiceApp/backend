package com.love.yourvoiceback.external.supertone.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SupertoneCreateClonedVoiceResponse(
        @JsonProperty("voice_id")
        String voiceId
) {
}
