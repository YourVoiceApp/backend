package com.love.yourvoiceback.room.controller;

import com.love.yourvoiceback.room.controller.dto.request.RoomCreateRequest;
import com.love.yourvoiceback.room.controller.dto.response.RoomResponse;
import com.love.yourvoiceback.room.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
           @AuthenticationPrincipal UserDetails userDetails
    ) {

    }
}
