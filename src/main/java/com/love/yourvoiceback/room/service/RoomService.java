package com.love.yourvoiceback.room.service;

import com.love.yourvoiceback.common.exception.ApiException;
import com.love.yourvoiceback.common.exception.ErrorCode;
import com.love.yourvoiceback.room.controller.dto.request.RoomCreateRequest;
import com.love.yourvoiceback.room.controller.dto.request.RoomJoinRequest;
import com.love.yourvoiceback.room.controller.dto.request.RoomUpdateRequest;
import com.love.yourvoiceback.room.controller.dto.response.RoomBrowsePageResponse;
import com.love.yourvoiceback.room.controller.dto.response.RoomBrowseResponse;
import com.love.yourvoiceback.room.controller.dto.response.RoomMemberResponse;
import com.love.yourvoiceback.room.controller.dto.response.RoomResponse;
import com.love.yourvoiceback.room.domain.RoomMembership;
import com.love.yourvoiceback.room.domain.VoiceRoom;
import com.love.yourvoiceback.room.enums.JoinPolicy;
import com.love.yourvoiceback.room.enums.MembershipRole;
import com.love.yourvoiceback.room.enums.MembershipStatus;
import com.love.yourvoiceback.room.reopository.RoomMembershipRepository;
import com.love.yourvoiceback.room.reopository.RoomRepository;
import com.love.yourvoiceback.room.reopository.RoomVoiceShareRepository;
import com.love.yourvoiceback.user.User;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoomService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_BROWSE_PAGE_SIZE = 50;

    private final RoomRepository roomRepository;
    private final RoomMembershipRepository roomMembershipRepository;
    private final RoomVoiceShareRepository roomVoiceShareRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RoomResponse createRoom(RoomCreateRequest request, User user) {
        validateCreateRoomRequest(request);

        VoiceRoom voiceRoom = VoiceRoom.of(
                user,
                request.getTitle().trim(),
                generateInviteCode(),
                request.getJoinPolicy(),
                resolvePasswordHash(request),
                request.getMaxParticipants()
        );
        VoiceRoom savedVoiceRoom = roomRepository.save(voiceRoom);
        RoomMembership roomMembership = RoomMembership.of(user, savedVoiceRoom);
        roomMembershipRepository.save(roomMembership);
        return RoomResponse.from(savedVoiceRoom);
    }

    @Transactional
    public RoomResponse joinRoom(RoomJoinRequest request, User user) {
        VoiceRoom room = resolveRoomForJoin(request);
        validateJoinRequest(room, request.password());

        RoomMembership existingMembership = roomMembershipRepository.findByRoomIdAndUserId(room.getId(), user.getId())
                .orElse(null);
        if (existingMembership != null) {
            return RoomResponse.from(rejoinRoom(existingMembership, room));
        }

        ensureRoomHasCapacity(room.getId(), room.getMaxParticipants());
        roomMembershipRepository.save(RoomMembership.join(user, room));
        return RoomResponse.from(room);
    }

    @Transactional(readOnly = true)
    public RoomBrowsePageResponse browseRooms(Pageable pageable) {
        int size = Math.min(Math.max(1, pageable.getPageSize()), MAX_BROWSE_PAGE_SIZE);
        int page = Math.max(0, pageable.getPageNumber());
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<VoiceRoom> roomPage = roomRepository.findAllByOrderByCreatedAtDesc(pageRequest);
        List<VoiceRoom> rooms = roomPage.getContent();
        Map<Long, Long> activeCounts = resolveActiveMemberCounts(rooms);

        List<RoomBrowseResponse> items = rooms.stream()
                .map(room -> RoomBrowseResponse.from(room, activeCounts.getOrDefault(room.getId(), 0L)))
                .toList();

        return new RoomBrowsePageResponse(
                items,
                roomPage.getTotalElements(),
                roomPage.getTotalPages(),
                roomPage.getNumber(),
                roomPage.getSize(),
                roomPage.isFirst(),
                roomPage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getMyRooms(User user) {
        return roomMembershipRepository.findAllByUserIdAndStatusOrderByJoinedAtDesc(user.getId(), MembershipStatus.ACTIVE).stream()
                .map(RoomMembership::getRoom)
                .map(RoomResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomResponse getMyRoom(Long roomId, User user) {
        return RoomResponse.from(getAccessibleRoom(roomId, user.getId()));
    }

    @Transactional(readOnly = true)
    public List<RoomMemberResponse> getRoomMembers(Long roomId, User user) {
        getAccessibleRoom(roomId, user.getId());
        return roomMembershipRepository.findAllByRoomIdAndStatusOrderByJoinedAtAsc(roomId, MembershipStatus.ACTIVE).stream()
                .map(RoomMemberResponse::from)
                .toList();
    }

    @Transactional
    public RoomResponse updateRoom(Long roomId, RoomUpdateRequest request, User user) {
        VoiceRoom room = getOwnedRoom(roomId, user.getId());

        validateUpdateRoomRequest(request, room);

        room.setName(request.getTitle().trim());
        room.setJoinPolicy(request.getJoinPolicy());
        room.setPasswordHash(resolveUpdatedPasswordHash(request, room));
        room.setMaxParticipants(request.getMaxParticipants());
        room.setUpdatedAt(LocalDateTime.now());

        return RoomResponse.from(room);
    }

    @Transactional
    public void deleteRoom(Long roomId, User user) {
        VoiceRoom room = getOwnedRoom(roomId, user.getId());

        roomVoiceShareRepository.deleteAllByRoomId(room.getId());
        roomMembershipRepository.deleteAllByRoomId(room.getId());
        roomRepository.delete(room);
    }

    private void validateCreateRoomRequest(RoomCreateRequest request) {
        if (request.getJoinPolicy() == JoinPolicy.INVITE_CODE_WITH_PASSWORD
                && (request.getPassword() == null || request.getPassword().isBlank())) {
            throw ApiException.error(ErrorCode.INVALID_REQUEST, "Password is required for password-protected rooms");
        }

        if (request.getJoinPolicy() == JoinPolicy.INVITE_CODE_ONLY
                && request.getPassword() != null
                && !request.getPassword().isBlank()) {
            throw ApiException.error(ErrorCode.INVALID_REQUEST, "Password is only allowed for password-protected rooms");
        }
    }

    private void validateUpdateRoomRequest(RoomUpdateRequest request, VoiceRoom room) {
        if (request.getJoinPolicy() == JoinPolicy.INVITE_CODE_WITH_PASSWORD
                && !hasPasswordForProtectedRoom(request, room)) {
            throw ApiException.error(ErrorCode.INVALID_REQUEST, "Password is required for password-protected rooms");
        }

        if (request.getJoinPolicy() == JoinPolicy.INVITE_CODE_ONLY
                && request.getPassword() != null
                && !request.getPassword().isBlank()) {
            throw ApiException.error(ErrorCode.INVALID_REQUEST, "Password is only allowed for password-protected rooms");
        }
    }

    private String resolvePasswordHash(RoomCreateRequest request) {
        if (request.getJoinPolicy() == JoinPolicy.INVITE_CODE_ONLY) {
            return null;
        }

        return passwordEncoder.encode(request.getPassword());
    }

    private String resolveUpdatedPasswordHash(RoomUpdateRequest request, VoiceRoom room) {
        if (request.getJoinPolicy() == JoinPolicy.INVITE_CODE_ONLY) {
            return null;
        }

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            return room.getPasswordHash();
        }

        return passwordEncoder.encode(request.getPassword());
    }

    private boolean hasPasswordForProtectedRoom(RoomUpdateRequest request, VoiceRoom room) {
        return (request.getPassword() != null && !request.getPassword().isBlank())
                || (room.getPasswordHash() != null && !room.getPasswordHash().isBlank());
    }

    private VoiceRoom getOwnedRoom(Long roomId, Long ownerId) {
        return roomRepository.findByIdAndOwnerId(roomId, ownerId)
                .orElseThrow(() -> ApiException.error(ErrorCode.ROOM_NOT_FOUND));
    }

    private VoiceRoom getAccessibleRoom(Long roomId, Long userId) {
        if (!roomMembershipRepository.existsByRoomIdAndUserIdAndStatus(roomId, userId, MembershipStatus.ACTIVE)) {
            throw ApiException.error(ErrorCode.ROOM_NOT_FOUND);
        }
        return roomRepository.findById(roomId)
                .orElseThrow(() -> ApiException.error(ErrorCode.ROOM_NOT_FOUND));
    }

    private VoiceRoom resolveRoomForJoin(RoomJoinRequest request) {
        boolean hasInvite = StringUtils.hasText(request.inviteCode());
        boolean hasRoomId = request.roomId() != null;
        if (hasInvite && hasRoomId) {
            throw ApiException.error(ErrorCode.INVALID_REQUEST, "Provide either inviteCode or roomId, not both");
        }
        if (!hasInvite && !hasRoomId) {
            throw ApiException.error(ErrorCode.INVALID_REQUEST, "Either inviteCode or roomId is required");
        }
        if (hasRoomId) {
            VoiceRoom room = roomRepository.findById(request.roomId())
                    .orElseThrow(() -> ApiException.error(ErrorCode.ROOM_NOT_FOUND));
            if (room.getJoinPolicy() != JoinPolicy.INVITE_CODE_WITH_PASSWORD) {
                throw ApiException.error(
                        ErrorCode.INVALID_REQUEST,
                        "Password-only join is only for password-protected rooms; use invite code for this room"
                );
            }
            return room;
        }

        String trimmed = request.inviteCode().trim();
        if (!trimmed.matches("\\d{6}")) {
            throw ApiException.error(ErrorCode.INVALID_REQUEST, "Invite code must be a 6-digit number");
        }
        return roomRepository.findByInviteCode(parseInviteCode(trimmed))
                .orElseThrow(() -> ApiException.error(ErrorCode.ROOM_NOT_FOUND));
    }

    private Map<Long, Long> resolveActiveMemberCounts(List<VoiceRoom> rooms) {
        if (rooms.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = rooms.stream().map(VoiceRoom::getId).toList();
        Map<Long, Long> counts = new HashMap<>();
        for (RoomMembershipRepository.ActiveMemberCountProjection row
                : roomMembershipRepository.countActiveMembersByRoomIds(ids, MembershipStatus.ACTIVE)) {
            Long cnt = row.getMemberCount();
            counts.put(row.getRoomId(), cnt != null ? cnt : 0L);
        }
        return counts;
    }

    private Integer generateInviteCode() {
        Integer inviteCode;
        do {
            inviteCode = 100000 + RANDOM.nextInt(900000);
        } while (roomRepository.existsByInviteCode(inviteCode));
        return inviteCode;
    }

    private int parseInviteCode(String inviteCode) {
        try {
            return Integer.parseInt(inviteCode);
        } catch (NumberFormatException exception) {
            throw ApiException.error(ErrorCode.INVALID_REQUEST, "Invite code must be a 6-digit number");
        }
    }

    private void validateJoinRequest(VoiceRoom room, String password) {
        if (room.getJoinPolicy() == JoinPolicy.INVITE_CODE_WITH_PASSWORD) {
            if (password == null || password.isBlank()) {
                throw ApiException.error(ErrorCode.INVALID_REQUEST, "Password is required for password-protected rooms");
            }
            if (!passwordEncoder.matches(password, room.getPasswordHash())) {
                throw ApiException.error(ErrorCode.INVALID_REQUEST, "Room password is incorrect");
            }
        }
    }

    private VoiceRoom rejoinRoom(RoomMembership membership, VoiceRoom room) {
        if (membership.getStatus() == MembershipStatus.BLOCKED) {
            throw ApiException.error(ErrorCode.INVALID_REQUEST, "Blocked users cannot join this room");
        }
        return room;
    }

    private void ensureRoomHasCapacity(Long roomId, Long maxParticipants) {
        long activeMembers = roomMembershipRepository.countByRoomIdAndStatus(roomId, MembershipStatus.ACTIVE);
        if (activeMembers >= maxParticipants) {
            throw ApiException.error(ErrorCode.INVALID_REQUEST, "Room is full");
        }
    }
}
