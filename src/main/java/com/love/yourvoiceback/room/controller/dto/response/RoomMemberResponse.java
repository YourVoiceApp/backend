package com.love.yourvoiceback.room.controller.dto.response;

import com.love.yourvoiceback.room.domain.RoomMembership;
import com.love.yourvoiceback.room.enums.MembershipRole;

public record RoomMemberResponse(
        Long id,
        String displayName,
        MembershipRole role
) {
    public static RoomMemberResponse from(RoomMembership membership) {
        return new RoomMemberResponse(
                membership.getUser().getId(),
                membership.getUser().getNickName(),
                membership.getRole()
        );
    }
}
