package com.love.yourvoiceback.room.controller.dto.response;

import com.love.yourvoiceback.room.domain.VoiceRoom;
import com.love.yourvoiceback.room.enums.JoinPolicy;

import java.time.LocalDateTime;

public record RoomBrowseResponse(
        Long id,
        String name,
        JoinPolicy joinPolicy,
        boolean passwordProtected,
        long activeMemberCount,
        Long maxParticipants,
        LocalDateTime createdAt
) {
    public static RoomBrowseResponse from(VoiceRoom room, long activeMemberCount) {
        boolean passwordProtected = room.getJoinPolicy() == JoinPolicy.PASSWORD_PROTECTED;
        return new RoomBrowseResponse(
                room.getId(),
                room.getName(),
                room.getJoinPolicy(),
                passwordProtected,
                activeMemberCount,
                room.getMaxParticipants(),
                room.getCreatedAt()
        );
    }
}
