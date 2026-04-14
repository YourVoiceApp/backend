package com.love.yourvoiceback.voice;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VoiceAssetRepository extends JpaRepository<VoiceAsset, Long> {
    boolean existsByExternalVoiceId(String externalVoiceId);
}
