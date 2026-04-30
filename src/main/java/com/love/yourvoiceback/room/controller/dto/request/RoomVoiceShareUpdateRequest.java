package com.love.yourvoiceback.room.controller.dto.request;

import com.love.yourvoiceback.room.enums.AccessScope;
import jakarta.validation.constraints.NotNull;

public record RoomVoiceShareUpdateRequest(
        @NotNull AccessScope accessScope,
        /**
         * null이면 표시 이름은 변경하지 않는다. 공백만 있으면 커스텀 이름을 지우고 보이스 자산 기본 제목으로 되돌린다.
         */
        String shareDisplayTitle
) {
}
