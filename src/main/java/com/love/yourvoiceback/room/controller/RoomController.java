package com.love.yourvoiceback.room.controller;

import com.love.yourvoiceback.common.security.CurrentUser;
import com.love.yourvoiceback.room.controller.dto.request.RoomCreateRequest;
import com.love.yourvoiceback.room.controller.dto.request.RoomUpdateRequest;
import com.love.yourvoiceback.room.controller.dto.response.RoomResponse;
import com.love.yourvoiceback.room.service.RoomService;
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

@RequiredArgsConstructor
@RestController
@RequestMapping("/room")
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    @Operation(summary = "방을 생성합니다.")
    public ResponseEntity<RoomResponse> createRoom(
           @Valid @RequestBody RoomCreateRequest request,
           @CurrentUser User user

    ) {
        RoomResponse response = roomService.createRoom(request, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "내가 만든 방 목록을 조회합니다.")
    public ResponseEntity<List<RoomResponse>> getMyRooms(@CurrentUser User user) {
        return ResponseEntity.ok(roomService.getMyRooms(user));
    }

    @GetMapping("/{roomId}")
    @Operation(summary = "내가 만든 방 정보를 조회합니다.")
    public ResponseEntity<RoomResponse> getMyRoom(
            @PathVariable Long roomId,
            @CurrentUser User user
    ) {
        return ResponseEntity.ok(roomService.getMyRoom(roomId, user));
    }

    @PutMapping("/{roomId}")
    @Operation(summary = "내가 만든 방 정보를 수정합니다.")
    public ResponseEntity<RoomResponse> updateRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody RoomUpdateRequest request,
            @CurrentUser User user
    ) {
        return ResponseEntity.ok(roomService.updateRoom(roomId, request, user));
    }

    @DeleteMapping("/{roomId}")
    @Operation(summary = "내가 만든 방을 삭제합니다.")
    public ResponseEntity<Void> deleteRoom(
            @PathVariable Long roomId,
            @CurrentUser User user
    ) {
        roomService.deleteRoom(roomId, user);
        return ResponseEntity.noContent().build();
    }
}
