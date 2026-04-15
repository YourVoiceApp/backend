package com.love.yourvoiceback.voice.repository;

import com.love.yourvoiceback.voice.domain.VoiceAsset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoiceAssetRepository extends JpaRepository<VoiceAsset, Long> {
    boolean existsByExternalVoiceId(String externalVoiceId);
}
