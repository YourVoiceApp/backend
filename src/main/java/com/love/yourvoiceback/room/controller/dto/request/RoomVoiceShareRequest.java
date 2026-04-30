package com.love.yourvoiceback.room.controller.dto.request;

import com.love.yourvoiceback.room.enums.AccessScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record RoomVoiceShareRequest(
        @NotEmpty List<String> externalVoiceIds,
        @NotNull AccessScope accessScope,
        @Schema(
                description = "외부 보이스 ID를 키로, 방 공유 화면에 보일 이름을 값으로 둔 객체. 키는 반드시 externalVoiceIds에 포함된 ID만 사용. 생략하거나 null이면 자산 기본 제목을 씀.",
                example = "{\"voice_ext_id_1\":\"회의용 이름\",\"voice_ext_id_2\":\"별칭\"}"
        )
        Map<String, String> shareDisplayTitlesByExternalVoiceId
) {
}