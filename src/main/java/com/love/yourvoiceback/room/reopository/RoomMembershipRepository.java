package com.love.yourvoiceback.room.reopository;

import com.love.yourvoiceback.room.domain.RoomMembership;
import com.love.yourvoiceback.room.enums.MembershipStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomMembershipRepository extends JpaRepository<RoomMembership, Long> {
    void deleteAllByRoomId(Long roomId);

    boolean existsByRoomIdAndUserIdAndStatus(Long roomId, Long userId, MembershipStatus status);

    @EntityGraph(attributePaths = {"room", "room.owner"})
    List<RoomMembership> findAllByUserIdAndStatusOrderByJoinedAtDesc(Long userId, MembershipStatus status);

    @EntityGraph(attributePaths = {"user"})
    List<RoomMembership> findAllByRoomIdAndStatusOrderByJoinedAtAsc(Long roomId, MembershipStatus status);

    Optional<RoomMembership> findByRoomIdAndUserId(Long roomId, Long userId);

    long countByRoomIdAndStatus(Long roomId, MembershipStatus status);
}
