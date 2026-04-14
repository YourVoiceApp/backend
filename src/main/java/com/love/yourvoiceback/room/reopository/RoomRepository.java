package com.love.yourvoiceback.room.reopository;

import com.love.yourvoiceback.room.domain.VoiceRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<VoiceRoom,Long>{
}
