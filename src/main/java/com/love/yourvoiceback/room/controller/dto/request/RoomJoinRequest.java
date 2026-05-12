package com.love.yourvoiceback.room.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 입장 방식 둘 중 하나만 사용합니다.
 * <ul>
 *   <li>{@code inviteCode}: 6자리 숫자 초대 코드로 방 조회 후 입장</li>
 *   <li>{@code roomId}: 비밀번호 방({@code INVITE_CODE_WITH_PASSWORD})만 — 초대 코드 없이 비밀번호만으로 입장</li>
 * </ul>
 */
@Schema(description = "inviteCode 또는 roomId 중 하나 필수. 둘 다 보내면 안 됩니다.")
public record RoomJoinRequest(
        @Schema(description = "6자리 숫자 문자열", example = "720341")
        String inviteCode,
        @Schema(description = "목록 조회로 얻은 방 ID (비밀번호 방 전용)", example = "1")
        Long roomId,
        @Schema(description = "비밀번호 방일 때 필수")
        String password
) {
}
