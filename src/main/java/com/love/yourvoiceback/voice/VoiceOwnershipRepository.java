package com.love.yourvoiceback.voice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface VoiceOwnershipRepository extends JpaRepository<VoiceOwnership, Long> {
    List<VoiceOwnership> findAllByUserIdAndVoiceAssetIdIn(Long userId, Collection<Long> voiceAssetIds);
}
