package com.love.yourvoiceback.room.service;

import com.love.yourvoiceback.common.exception.ApiException;
import com.love.yourvoiceback.common.exception.ErrorCode;
import com.love.yourvoiceback.room.controller.dto.request.RoomCreateRequest;
import com.love.yourvoiceback.room.controller.dto.response.RoomResponse;
import com.love.yourvoiceback.room.domain.VoiceRoom;
import com.love.yourvoiceback.room.enums.JoinPolicy;
import com.love.yourvoiceback.room.reopository.RoomRepository;
import com.love.yourvoiceback.user.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class RoomService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RoomRepository roomRepository;
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
        return RoomResponse.from(savedVoiceRoom);
    }

    //비지니스 요구상 사용자가 비밀번호를 누를경우 초대 코드 + 비밀번호 저장 느낌이라 생각
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

    private String resolvePasswordHash(RoomCreateRequest request) {
        if (request.getJoinPolicy() == JoinPolicy.INVITE_CODE_ONLY) {
            return null;
        }

        return passwordEncoder.encode(request.getPassword());
    }

    private Integer generateInviteCode() {
        return 100000 + RANDOM.nextInt(900000);
    }
}
