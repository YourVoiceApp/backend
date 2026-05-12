package com.love.yourvoiceback.room.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * {@link #roomId}는 목록({@code GET /room/discover}) 등에서 선택한 방의 ID.
 * 비밀번호 방은 {@link #password} 필수, 공개 방은 비밀번호를 보내지 마세요.
 */
@Schema(description = "방 입장: roomId 필수. PASSWORD_PROTECTED 방만 password 필요.")
public record RoomJoinRequest(
        @NotNull
        @Schema(description = "입장할 방 ID", example = "6")
        Long roomId,
        @Schema(description = "비밀번호 방일 때 필수", example = "1234")
        String password
) {
}
