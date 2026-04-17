package com.love.yourvoiceback.room.reopository;

import com.love.yourvoiceback.room.domain.VoiceRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<VoiceRoom,Long>{
    List<VoiceRoom> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<VoiceRoom> findByIdAndOwnerId(Long id, Long ownerId);

    Optional<VoiceRoom> findByInviteCode(Integer inviteCode);

    boolean existsByInviteCode(Integer inviteCode);
}
