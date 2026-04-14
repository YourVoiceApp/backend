package com.love.yourvoiceback.room.domain;

import com.love.yourvoiceback.room.enums.MembershipRole;
import com.love.yourvoiceback.room.enums.MembershipStatus;
import com.love.yourvoiceback.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/*
* VoiceRoom - RoomMemberShip - User  1:N:1 관계 의미
* */

@Entity
@Table(
        name = "room_membership",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_room_membership_room_user", columnNames = {"room_id", "user_id"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private VoiceRoom room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MembershipRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MembershipStatus status;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    public static RoomMembership of (User user, VoiceRoom savedVoiceRoom) {
        return RoomMembership.builder()
                .room(savedVoiceRoom)
                .user(user)
                .role(MembershipRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build();
    }
}
