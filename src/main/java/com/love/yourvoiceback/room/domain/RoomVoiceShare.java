package com.love.yourvoiceback.room.domain;

import com.love.yourvoiceback.room.enums.AccessScope;
import com.love.yourvoiceback.voice.VoiceAsset;
import jakarta.persistence.*;
import lombok.*;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccessScope accessScope;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime sharedAt = LocalDateTime.now();

}
