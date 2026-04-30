package com.love.yourvoiceback.room.controller.dto.request;

import com.love.yourvoiceback.room.enums.AccessScope;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record RoomVoiceShareRequest(
        @NotEmpty List<String> externalVoiceIds,
        @NotNull AccessScope accessScope,
        /**
         * 키: {@code externalVoiceIds}에 포함된 보이스 ID. 값: 방에서 표시할 이름(선택).
         */
        Map<String, String> shareDisplayTitlesByExternalVoiceId
) {
}
