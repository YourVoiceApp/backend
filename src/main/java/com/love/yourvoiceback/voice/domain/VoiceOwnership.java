package com.love.yourvoiceback.voice.domain;

import com.love.yourvoiceback.market.VoicePurchase;
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
import jakarta.persistence.OneToOne;
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id", unique = true)
    private VoicePurchase purchase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AcquisitionType acquiredBy;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime acquiredAt = LocalDateTime.now();

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

    public enum AcquisitionType {
        CREATED,
        ROOM_SHARED,
        MARKET_PURCHASED,
        ADMIN_GRANTED
    }
}
