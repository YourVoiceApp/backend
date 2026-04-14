package com.love.yourvoiceback.room.domain;

import com.love.yourvoiceback.room.enums.JoinPolicy;
import com.love.yourvoiceback.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/*
* VoiceRoom - User N:N 관계
* */

@Entity
@Table(name = "voice_room")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50, unique = true)
    private String inviteCode;

    @Column(length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private JoinPolicy joinPolicy;

    @Column(nullable = false)
    private Long maxParticipants;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public static VoiceRoom of(User owner, String name, String inviteCode, JoinPolicy joinPolicy, String passwordHash, Long maxParticipants) {
        return VoiceRoom.builder()
                .owner(owner)
                .name(name)
                .inviteCode(inviteCode)
                .joinPolicy(joinPolicy)
                .passwordHash(passwordHash)
                .maxParticipants(maxParticipants)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
