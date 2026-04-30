package com.love.yourvoiceback.room.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record RoomVoiceShareDisplayTitleRequest(
        @NotNull
        @Schema(
                description = "방 공유 목록에 보일 이름. 빈 문자열이면 커스텀 이름을 지우고 음성 자산 기본 제목으로 표시합니다.",
                example = "회의용 별칭"
        )
        String shareDisplayTitle
) {
}
