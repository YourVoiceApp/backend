package com.love.yourvoiceback.external.supertone.dto;

import org.springframework.http.MediaType;

public record SupertoneTextToSpeechResponse(
        byte[] body,
        MediaType contentType
) {
}
