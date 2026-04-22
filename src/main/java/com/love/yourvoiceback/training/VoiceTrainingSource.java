package com.love.yourvoiceback.training;

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
@Table(name = "voice_training_source")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceTrainingSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    private User uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voice_asset_id")
    private VoiceAsset voiceAsset;

    @Column(nullable = false, length = 500, unique = true)
    private String storagePath;

    @Column(nullable = false, length = 255)
    private String fileName;

    private Long fileSize;

    @Column(length = 64)
    private String checksum;

    private Long durationMs;

    @Column(length = 100)
    private String contentType;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    public void attachToVoiceAsset(VoiceAsset voiceAsset) {
        this.voiceAsset = voiceAsset;
    }
}
