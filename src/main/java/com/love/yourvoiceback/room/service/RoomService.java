package com.love.yourvoiceback.room.service;

import com.love.yourvoiceback.common.exception.ApiException;
import com.love.yourvoiceback.common.exception.ErrorCode;
import com.love.yourvoiceback.room.controller.dto.request.RoomCreateRequest;
import com.love.yourvoiceback.room.controller.dto.request.RoomUpdateRequest;
import com.love.yourvoiceback.room.controller.dto.response.RoomResponse;
import com.love.yourvoiceback.room.domain.RoomMembership;
import com.love.yourvoiceback.room.domain.VoiceRoom;
import com.love.yourvoiceback.room.enums.JoinPolicy;
import com.love.yourvoiceback.room.reopository.RoomMembershipRepository;
import com.love.yourvoiceback.room.reopository.RoomRepository;
import com.love.yourvoiceback.room.reopository.RoomVoiceShareRepository;
import com.love.yourvoiceback.user.User;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {
    private static final SecureRandom RANDOM = new SecureRandom();

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

    @Transactional(readOnly = true)
    public List<RoomResponse> getMyRooms(User user) {
        return roomRepository.findAllByOwnerIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(RoomResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomResponse getMyRoom(Long roomId, User user) {
        return RoomResponse.from(getOwnedRoom(roomId, user.getId()));
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

    private Integer generateInviteCode() {
        return 100000 + RANDOM.nextInt(900000);
    }
}
