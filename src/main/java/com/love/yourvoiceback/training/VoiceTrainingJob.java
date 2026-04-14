package com.love.yourvoiceback.training;

import com.love.yourvoiceback.user.User;
import com.love.yourvoiceback.voice.VoiceFolder;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "voice_training_job")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceTrainingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_user_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_file_id", nullable = false)
    private VoiceTrainingSource sourceFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_folder_id")
    private VoiceFolder requestedFolder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TrainingStatus status;

    @Column(length = 1000)
    private String failureReason;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    public enum TrainingStatus {
        UPLOADED,
        VALIDATING,
        TRAINING,
        COMPLETED,
        FAILED
    }
}
