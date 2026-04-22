package com.love.yourvoiceback.inference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "generated_audio")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedAudio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private SpeechSynthesisRequest request;

    @Column(nullable = false, length = 500)
    private String audioUrl;

    @Column(length = 64)
    private String requestHash;

    @Column(length = 100)
    private String audioContentType;

    private Long durationMs;

    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public static GeneratedAudio create(
            SpeechSynthesisRequest request,
            String audioUrl,
            String requestHash,
            String audioContentType
    ) {
        return GeneratedAudio.builder()
                .request(request)
                .audioUrl(audioUrl)
                .requestHash(requestHash)
                .audioContentType(audioContentType)
                .build();
    }

    public void updateAudioReference(String audioUrl, String requestHash, String audioContentType) {
        this.audioUrl = audioUrl;
        this.requestHash = requestHash;
        this.audioContentType = audioContentType;
    }
}
