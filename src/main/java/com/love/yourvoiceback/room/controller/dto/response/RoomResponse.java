package com.love.yourvoiceback.room.controller.dto.response;

import com.love.yourvoiceback.room.domain.VoiceRoom;
import com.love.yourvoiceback.room.enums.JoinPolicy;

import java.time.LocalDateTime;

public record RoomResponse(
        Long id,
        Long ownerId,
        String name,
        Integer inviteCode,
        JoinPolicy joinPolicy,
        Long maxParticipants,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static RoomResponse from(VoiceRoom voiceRoom) {
        return new RoomResponse(
                voiceRoom.getId(),
                voiceRoom.getOwner().getId(),
                voiceRoom.getName(),
                voiceRoom.getInviteCode(),
                voiceRoom.getJoinPolicy(),
                voiceRoom.getMaxParticipants(),
                voiceRoom.getCreatedAt(),
                voiceRoom.getUpdatedAt()
        );
    }
}
