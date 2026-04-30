package com.love.yourvoiceback.room.service;

import com.love.yourvoiceback.common.exception.ApiException;
import com.love.yourvoiceback.common.exception.ErrorCode;
import com.love.yourvoiceback.room.controller.dto.request.RoomVoiceShareDisplayTitleRequest;
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
import com.love.yourvoiceback.voice.domain.VoiceAsset;
import com.love.yourvoiceback.voice.domain.VoiceOwnership;
import com.love.yourvoiceback.voice.repository.VoiceOwnershipRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
        validateShareDisplayTitleMap(request.externalVoiceIds(), request.shareDisplayTitlesByExternalVoiceId());
        List<VoiceAsset> voiceAssets = getOwnedVoiceAssets(roomId, request.externalVoiceIds(), user.getId());
        Map<String, String> titleByVoiceId = request.shareDisplayTitlesByExternalVoiceId() != null
                ? request.shareDisplayTitlesByExternalVoiceId()
                : Map.of();

        return voiceAssets.stream()
                .map(voiceAsset -> {
                    String shareTitle = normalizeShareDisplayTitle(titleByVoiceId.get(voiceAsset.getExternalVoiceId()));
                    return createRoomVoiceShare(room, voiceAsset, request.accessScope(), shareTitle);
                })
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
        applyShareDisplayTitleUpdate(roomVoiceShare, request.shareDisplayTitle());

        return RoomVoiceShareResponse.from(roomVoiceShare);
    }

    @Transactional
    public RoomVoiceShareResponse patchRoomVoiceShareDisplayTitle(
            Long roomId,
            Long shareId,
            RoomVoiceShareDisplayTitleRequest request,
            User user
    ) {
        getAccessibleRoom(roomId, user.getId());

        RoomVoiceShare roomVoiceShare = getRoomVoiceShare(roomId, shareId);
        replaceShareDisplayTitle(roomVoiceShare, request.shareDisplayTitle());

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

    private List<VoiceAsset> getOwnedVoiceAssets(Long roomId, List<String> externalVoiceIds, Long userId) {
        Set<String> uniqueExternalVoiceIds = new LinkedHashSet<>(externalVoiceIds);
        if (uniqueExternalVoiceIds.size() != externalVoiceIds.size()) {
            throw ApiException.error(ErrorCode.INVALID_REQUEST, "Duplicate external voice ids are not allowed");
        }

        List<VoiceOwnership> voiceOwnerships = voiceOwnershipRepository.findAllByUserIdAndVoiceAssetExternalVoiceIdIn(
                userId,
                uniqueExternalVoiceIds
        );
        if (voiceOwnerships.size() != uniqueExternalVoiceIds.size()) {
            throw ApiException.error(ErrorCode.VOICE_ASSET_NOT_FOUND);
        }

        List<VoiceAsset> voiceAssets = voiceOwnerships.stream()
                .map(VoiceOwnership::getVoiceAsset)
                .toList();

        for (VoiceAsset voiceAsset : voiceAssets) {
            if (roomVoiceShareRepository.existsByRoomIdAndVoiceAssetExternalVoiceId(roomId, voiceAsset.getExternalVoiceId())) {
                throw ApiException.error(
                        ErrorCode.INVALID_REQUEST,
                        "Voice asset is already shared in this room: " + voiceAsset.getExternalVoiceId()
                );
            }
        }

        return voiceAssets;
    }

    private RoomVoiceShare createRoomVoiceShare(
            VoiceRoom room,
            VoiceAsset voiceAsset,
            @NotNull AccessScope accessScope,
            String shareDisplayTitle
    ) {
        return RoomVoiceShare.builder()
                .room(room)
                .voiceAsset(voiceAsset)
                .accessScope(accessScope)
                .shareDisplayTitle(shareDisplayTitle)
                .build();
    }

    private void validateShareDisplayTitleMap(List<String> externalVoiceIds, Map<String, String> titleMap) {
        if (titleMap == null || titleMap.isEmpty()) {
            return;
        }
        Set<String> allowed = new LinkedHashSet<>(externalVoiceIds);
        for (String key : titleMap.keySet()) {
            if (!allowed.contains(key)) {
                throw ApiException.error(
                        ErrorCode.INVALID_REQUEST,
                        "shareDisplayTitlesByExternalVoiceId contains unknown external voice id: " + key
                );
            }
        }
        for (String value : titleMap.values()) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            if (value.trim().length() > 100) {
                throw ApiException.error(ErrorCode.INVALID_REQUEST, "Share display title must be 100 characters or fewer");
            }
        }
    }

    private String normalizeShareDisplayTitle(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.length() > 100) {
            throw ApiException.error(ErrorCode.INVALID_REQUEST, "Share display title must be 100 characters or fewer");
        }
        return trimmed;
    }

    private void applyShareDisplayTitleUpdate(RoomVoiceShare roomVoiceShare, String shareDisplayTitle) {
        if (shareDisplayTitle == null) {
            return;
        }
        replaceShareDisplayTitle(roomVoiceShare, shareDisplayTitle);
    }

    private void replaceShareDisplayTitle(RoomVoiceShare roomVoiceShare, String shareDisplayTitle) {
        if (!StringUtils.hasText(shareDisplayTitle)) {
            roomVoiceShare.setShareDisplayTitle(null);
            return;
        }
        String trimmed = shareDisplayTitle.trim();
        if (trimmed.length() > 100) {
            throw ApiException.error(ErrorCode.INVALID_REQUEST, "Share display title must be 100 characters or fewer");
        }
        roomVoiceShare.setShareDisplayTitle(trimmed);
    }

    private RoomVoiceShare getRoomVoiceShare(Long roomId, Long shareId) {
        return roomVoiceShareRepository.findByIdAndRoomId(shareId, roomId)
                .orElseThrow(() -> ApiException.error(ErrorCode.ROOM_VOICE_SHARE_NOT_FOUND));
    }
}
