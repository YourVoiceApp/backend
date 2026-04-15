package com.love.yourvoiceback.inference;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeechSynthesisRequestRepository extends JpaRepository<SpeechSynthesisRequest, Long> {
}
