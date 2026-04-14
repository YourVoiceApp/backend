package com.love.yourvoiceback.room.reopository;

import com.love.yourvoiceback.room.domain.RoomVoiceShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomVoiceShareRepository extends JpaRepository<RoomVoiceShare, Long> {
    void deleteAllByRoomId(Long roomId);

    List<RoomVoiceShare> findAllByRoomIdOrderBySharedAtDesc(Long roomId);

    Optional<RoomVoiceShare> findByIdAndRoomId(Long id, Long roomId);

    boolean existsByRoomIdAndVoiceAssetId(Long roomId, Long voiceAssetId);
}
