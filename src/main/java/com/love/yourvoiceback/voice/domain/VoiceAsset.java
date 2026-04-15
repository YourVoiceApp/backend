package com.love.yourvoiceback.voice.domain;

import com.love.yourvoiceback.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "voice_asset")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_user_id", nullable = false)
    private User creator;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 100, unique = true)
    private String externalVoiceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VoiceAssetStatus status;

    @Column(length = 500)
    private String sampleAudioUrl;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public static VoiceAsset createCloned(User creator, String title, String externalVoiceId) {
        return VoiceAsset.builder()
                .creator(creator)
                .title(title)
                .externalVoiceId(externalVoiceId)
                .status(VoiceAssetStatus.TRAINING)
                .build();
    }

    public enum VoiceAssetStatus {
        UPLOADED,
        VALIDATING,
        TRAINING,
        COMPLETED,
        FAILED,
        DELETED
    }
}
