package com.love.yourvoiceback.training;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "voice_model_artifact",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_voice_model_artifact_job_version", columnNames = {"training_job_id", "modelVersion"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceModelArtifact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "training_job_id", nullable = false)
    private VoiceTrainingJob trainingJob;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voice_asset_id", nullable = false)
    private VoiceAsset voiceAsset;

    @Column(nullable = false, length = 50)
    private String modelVersion;

    @Column(nullable = false, length = 500)
    private String artifactPath;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
