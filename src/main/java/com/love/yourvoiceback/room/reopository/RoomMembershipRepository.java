package com.love.yourvoiceback.room.reopository;

import com.love.yourvoiceback.room.domain.RoomMembership;
import com.love.yourvoiceback.room.enums.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomMembershipRepository extends JpaRepository<RoomMembership, Long> {
    void deleteAllByRoomId(Long roomId);

    boolean existsByRoomIdAndUserIdAndStatus(Long roomId, Long userId, MembershipStatus status);
}
