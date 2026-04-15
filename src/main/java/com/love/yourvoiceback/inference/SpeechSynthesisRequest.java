package com.love.yourvoiceback.inference;

import com.love.yourvoiceback.user.User;
import com.love.yourvoiceback.voice.domain.VoiceAsset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "speech_synthesis_request")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeechSynthesisRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voice_asset_id", nullable = false)
    private VoiceAsset voiceAsset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedBy;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public static SpeechSynthesisRequest createListenRequest(User requestedBy, VoiceAsset voiceAsset, String text) {
        return SpeechSynthesisRequest.builder()
                .requestedBy(requestedBy)
                .voiceAsset(voiceAsset)
                .text(text)
                .build();
    }
}
