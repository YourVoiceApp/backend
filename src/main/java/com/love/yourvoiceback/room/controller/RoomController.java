package com.love.yourvoiceback.room.controller;

import com.love.yourvoiceback.common.security.CurrentUser;
import com.love.yourvoiceback.room.controller.dto.request.RoomCreateRequest;
import com.love.yourvoiceback.room.controller.dto.response.RoomResponse;
import com.love.yourvoiceback.room.service.RoomService;
import com.love.yourvoiceback.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/room")
public class RoomController {

    private final RoomService roomService;

    @PostMapping("")
    public ResponseEntity<RoomResponse> createRoom(
           @Valid @RequestBody RoomCreateRequest request,
           @CurrentUser User user

    ) {
        RoomResponse response = roomService.createRoom(request, user);
        return ResponseEntity.ok(response);
    }
}
