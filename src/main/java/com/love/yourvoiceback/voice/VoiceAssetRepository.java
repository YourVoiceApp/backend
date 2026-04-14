package com.love.yourvoiceback.voice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface VoiceAssetRepository extends JpaRepository<VoiceAsset, Long> {
    List<VoiceAsset> findAllByIdInAndOwnerId(Collection<Long> ids, Long ownerId);
}
