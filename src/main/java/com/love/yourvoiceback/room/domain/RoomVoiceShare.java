package com.love.yourvoiceback.room.domain;

import com.love.yourvoiceback.user.User;
import com.love.yourvoiceback.voice.VoiceAsset;
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

/*
 * VoiceRoom - RoomVoiceShare - Room  1:N:1 관계 의미
 * */

@Entity
@Table(
        name = "room_voice_share",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_room_voice_share_room_voice", columnNames = {"room_id", "voice_asset_id"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomVoiceShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private VoiceRoom room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voice_asset_id", nullable = false)
    private VoiceAsset voiceAsset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shared_by_user_id", nullable = false)
    private User sharedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccessScope accessScope;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime sharedAt = LocalDateTime.now();

    public enum AccessScope {
        LISTEN_ONLY,
        SYNTHESIS_ALLOWED,
        DOWNLOAD_ALLOWED
    }
}
