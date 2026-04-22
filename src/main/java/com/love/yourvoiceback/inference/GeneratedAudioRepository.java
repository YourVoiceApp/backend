package com.love.yourvoiceback.inference;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GeneratedAudioRepository extends JpaRepository<GeneratedAudio, Long> {
    Optional<GeneratedAudio> findFirstByRequestHashAndRequestVoiceAssetExternalVoiceIdOrderByIdDesc(
            String requestHash,
            String externalVoiceId
    );
}
