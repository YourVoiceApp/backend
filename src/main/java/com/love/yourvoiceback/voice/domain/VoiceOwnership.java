package com.love.yourvoiceback.voice.domain;

import com.love.yourvoiceback.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "voice_ownership",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_voice_ownership_user_asset", columnNames = {"user_id", "voice_asset_id"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceOwnership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voice_asset_id", nullable = false)
    private VoiceAsset voiceAsset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private VoiceFolder folder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AcquisitionType acquiredBy;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime acquiredAt = LocalDateTime.now();

    /**
     * 사용자 라이브러리에서만 쓰는 표시 이름. null이면 {@link VoiceAsset#getTitle()}을 그대로 사용한다.
     */
    @Column(length = 100)
    private String displayTitle;

    public static VoiceOwnership createCreatedOwnership(User user, VoiceAsset voiceAsset) {
        return VoiceOwnership.builder()
                .voiceAsset(voiceAsset)
                .user(user)
                .acquiredBy(AcquisitionType.CREATED)
                .build();
    }

    public void clearFolder() {
        this.folder = null;
    }

    public void changeFolder(VoiceFolder folder) {
        this.folder = folder;
    }

    public String resolveDisplayTitle() {
        if (displayTitle != null && !displayTitle.isBlank()) {
            return displayTitle.trim();
        }
        return voiceAsset.getTitle();
    }

    public enum AcquisitionType {
        CREATED,
        ROOM_SHARED,
        ADMIN_GRANTED
    }
}
