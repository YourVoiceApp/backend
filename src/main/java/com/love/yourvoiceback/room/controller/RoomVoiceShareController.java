package com.love.yourvoiceback.room.controller;

import com.love.yourvoiceback.common.security.CurrentUser;
import com.love.yourvoiceback.room.controller.dto.request.RoomVoiceShareRequest;
import com.love.yourvoiceback.room.controller.dto.request.RoomVoiceShareUpdateRequest;
import com.love.yourvoiceback.room.controller.dto.response.RoomVoiceShareResponse;
import com.love.yourvoiceback.room.service.RoomVoiceShareService;
import com.love.yourvoiceback.user.User;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/room/{roomId}/voice-shares")
@RequiredArgsConstructor
public class RoomVoiceShareController {

    private final RoomVoiceShareService roomVoiceShareService;

    @PostMapping
    @Operation(summary = "방에 음성을 공유합니다.")
    public ResponseEntity<List<RoomVoiceShareResponse>> createRoomVoiceShare(
            @PathVariable Long roomId,
            @Valid @RequestBody RoomVoiceShareRequest request,
            @CurrentUser User user
    ) {
        return ResponseEntity.ok(roomVoiceShareService.createRoomVoiceShares(roomId, request, user));
    }

    @GetMapping
    @Operation(summary = "방에 공유된 음성 목록을 조회합니다.")
    public ResponseEntity<List<RoomVoiceShareResponse>> getRoomVoiceShares(
            @PathVariable Long roomId,
            @CurrentUser User user
    ) {
        return ResponseEntity.ok(roomVoiceShareService.getRoomVoiceShares(roomId, user));
    }

    @GetMapping("/{shareId}")
    @Operation(summary = "공유된 음성 정보를 조회합니다.")
    public ResponseEntity<RoomVoiceShareResponse> getRoomVoiceShare(
            @PathVariable Long roomId,
            @PathVariable Long shareId,
            @CurrentUser User user
    ) {
        return ResponseEntity.ok(roomVoiceShareService.getRoomVoiceShare(roomId, shareId, user));
    }

    @PutMapping("/{shareId}")
    @Operation(summary = "공유된 음성의 접근 범위를 수정합니다.")
    public ResponseEntity<RoomVoiceShareResponse> updateRoomVoiceShare(
            @PathVariable Long roomId,
            @PathVariable Long shareId,
            @Valid @RequestBody RoomVoiceShareUpdateRequest request,
            @CurrentUser User user
    ) {
        return ResponseEntity.ok(roomVoiceShareService.updateRoomVoiceShare(roomId, shareId, request, user));
    }

    @DeleteMapping("/{shareId}")
    @Operation(summary = "공유된 음성을 삭제합니다.")
    public ResponseEntity<Void> deleteRoomVoiceShare(
            @PathVariable Long roomId,
            @PathVariable Long shareId,
            @CurrentUser User user
    ) {
        roomVoiceShareService.deleteRoomVoiceShare(roomId, shareId, user);
        return ResponseEntity.noContent().build();
    }
}
