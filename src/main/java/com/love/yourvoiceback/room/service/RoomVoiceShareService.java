package com.love.yourvoiceback.room.service;

import com.love.yourvoiceback.common.exception.ApiException;
import com.love.yourvoiceback.common.exception.ErrorCode;
import com.love.yourvoiceback.room.controller.dto.request.RoomVoiceShareRequest;
import com.love.yourvoiceback.room.controller.dto.request.RoomVoiceShareUpdateRequest;
import com.love.yourvoiceback.room.controller.dto.response.RoomVoiceShareResponse;
import com.love.yourvoiceback.room.domain.RoomVoiceShare;
import com.love.yourvoiceback.room.domain.VoiceRoom;
import com.love.yourvoiceback.room.enums.AccessScope;
import com.love.yourvoiceback.room.enums.MembershipStatus;
import com.love.yourvoiceback.room.reopository.RoomMembershipRepository;
import com.love.yourvoiceback.room.reopository.RoomRepository;
import com.love.yourvoiceback.room.reopository.RoomVoiceShareRepository;
import com.love.yourvoiceback.user.User;
import com.love.yourvoiceback.voice.VoiceAsset;
import com.love.yourvoiceback.voice.VoiceOwnership;
import com.love.yourvoiceback.voice.VoiceOwnershipRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoomVoiceShareService {

    private final RoomRepository roomRepository;
    private final RoomMembershipRepository roomMembershipRepository;
    private final RoomVoiceShareRepository roomVoiceShareRepository;
    private final VoiceOwnershipRepository voiceOwnershipRepository;

    @Transactional
    public List<RoomVoiceShareResponse> createRoomVoiceShares(Long roomId, RoomVoiceShareRequest request, User user) {
        VoiceRoom room = getAccessibleRoom(roomId, user.getId());
        List<VoiceAsset> voiceAssets = getOwnedVoiceAssets(roomId, request.voiceAssetIds(), user.getId());

        return voiceAssets.stream()
                .map(voiceAsset -> createRoomVoiceShare(room, voiceAsset, request.accessScope()))
                .map(roomVoiceShareRepository::save)
                .map(RoomVoiceShareResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoomVoiceShareResponse> getRoomVoiceShares(Long roomId, User user) {
        getAccessibleRoom(roomId, user.getId());

        return roomVoiceShareRepository.findAllByRoomIdOrderBySharedAtDesc(roomId).stream()
                .map(RoomVoiceShareResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomVoiceShareResponse getRoomVoiceShare(Long roomId, Long shareId, User user) {
        getAccessibleRoom(roomId, user.getId());
        return RoomVoiceShareResponse.from(getRoomVoiceShare(roomId, shareId));
    }

    @Transactional
    public RoomVoiceShareResponse updateRoomVoiceShare(
            Long roomId,
            Long shareId,
            RoomVoiceShareUpdateRequest request,
            User user
    ) {
        getAccessibleRoom(roomId, user.getId());

        RoomVoiceShare roomVoiceShare = getRoomVoiceShare(roomId, shareId);
        roomVoiceShare.setAccessScope(request.accessScope());

        return RoomVoiceShareResponse.from(roomVoiceShare);
    }

    @Transactional
    public void deleteRoomVoiceShare(Long roomId, Long shareId, User user) {
        getAccessibleRoom(roomId, user.getId());
        roomVoiceShareRepository.delete(getRoomVoiceShare(roomId, shareId));
    }

    private VoiceRoom getAccessibleRoom(Long roomId, Long userId) {
        if (!roomMembershipRepository.existsByRoomIdAndUserIdAndStatus(
                roomId,
                userId,
                MembershipStatus.ACTIVE
        )) {
            throw ApiException.error(ErrorCode.ROOM_NOT_FOUND);
        }

        return roomRepository.findById(roomId)
                .orElseThrow(() -> ApiException.error(ErrorCode.ROOM_NOT_FOUND));
    }

    private List<VoiceAsset> getOwnedVoiceAssets(Long roomId, List<Long> voiceAssetIds, Long userId) {
        Set<Long> uniqueVoiceAssetIds = new LinkedHashSet<>(voiceAssetIds);
        if (uniqueVoiceAssetIds.size() != voiceAssetIds.size()) {
            throw ApiException.error(ErrorCode.INVALID_REQUEST, "Duplicate voice asset ids are not allowed");
        }

        List<VoiceOwnership> voiceOwnerships = voiceOwnershipRepository.findAllByUserIdAndVoiceAssetIdIn(userId, uniqueVoiceAssetIds);
        if (voiceOwnerships.size() != uniqueVoiceAssetIds.size()) {
            throw ApiException.error(ErrorCode.VOICE_ASSET_NOT_FOUND);
        }

        List<VoiceAsset> voiceAssets = voiceOwnerships.stream()
                .map(VoiceOwnership::getVoiceAsset)
                .toList();

        for (VoiceAsset voiceAsset : voiceAssets) {
            if (roomVoiceShareRepository.existsByRoomIdAndVoiceAssetId(roomId, voiceAsset.getId())) {
                throw ApiException.error(
                        ErrorCode.INVALID_REQUEST,
                        "Voice asset is already shared in this room: " + voiceAsset.getId()
                );
            }
        }

        return voiceAssets;
    }

    private RoomVoiceShare createRoomVoiceShare(
            VoiceRoom room,
            VoiceAsset voiceAsset,
            @NotNull AccessScope accessScope
    ) {
        return RoomVoiceShare.builder()
                .room(room)
                .voiceAsset(voiceAsset)
                .accessScope(accessScope)
                .build();
    }

    private RoomVoiceShare getRoomVoiceShare(Long roomId, Long shareId) {
        return roomVoiceShareRepository.findByIdAndRoomId(shareId, roomId)
                .orElseThrow(() -> ApiException.error(ErrorCode.ROOM_VOICE_SHARE_NOT_FOUND));
    }
}
