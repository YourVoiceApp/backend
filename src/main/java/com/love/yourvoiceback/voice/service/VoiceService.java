package com.love.yourvoiceback.voice.service;

import com.love.yourvoiceback.common.exception.ApiException;
import com.love.yourvoiceback.common.exception.ErrorCode;
import com.love.yourvoiceback.external.supertone.SupertoneVoiceClient;
import com.love.yourvoiceback.external.supertone.dto.SupertoneCreateClonedVoiceResponse;
import com.love.yourvoiceback.user.User;
import com.love.yourvoiceback.voice.domain.VoiceAsset;
import com.love.yourvoiceback.voice.domain.VoiceFolder;
import com.love.yourvoiceback.voice.domain.VoiceOwnership;
import com.love.yourvoiceback.voice.dto.request.VoiceOwnershipFolderUpdateRequest;
import com.love.yourvoiceback.voice.dto.response.CreateClonedVoiceAssetResponse;
import com.love.yourvoiceback.voice.dto.response.OwnedVoiceAssetResponse;
import com.love.yourvoiceback.voice.repository.VoiceAssetRepository;
import com.love.yourvoiceback.voice.repository.VoiceFolderRepository;
import com.love.yourvoiceback.voice.repository.VoiceOwnershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VoiceService {

    private static final long MAX_FILE_SIZE_BYTES = 3L * 1024L * 1024L;

    private final SupertoneVoiceClient supertoneVoiceClient;
    private final VoiceAssetRepository voiceAssetRepository;
    private final VoiceOwnershipRepository voiceOwnershipRepository;
    private final VoiceFolderRepository voiceFolderRepository;

    @Transactional(readOnly = true)
    public List<OwnedVoiceAssetResponse> getOwnedVoices(User user, Long folderId) {
        if (folderId == null) {
            return voiceOwnershipRepository.findAllByUserIdOrderByAcquiredAtDesc(user.getId()).stream()
                    .map(OwnedVoiceAssetResponse::from)
                    .toList();
        }

        validateOwnedFolder(folderId, user.getId());

        return voiceOwnershipRepository.findAllByUserIdAndFolderIdOrderByAcquiredAtDesc(user.getId(), folderId).stream()
                .map(OwnedVoiceAssetResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OwnedVoiceAssetResponse> getUnassignedOwnedVoices(User user) {
        return voiceOwnershipRepository.findAllByUserIdAndFolderIsNullOrderByAcquiredAtDesc(user.getId()).stream()
                .map(OwnedVoiceAssetResponse::from)
                .toList();
    }

    @Transactional
    public List<OwnedVoiceAssetResponse> updateOwnedVoiceFolder(User user, VoiceOwnershipFolderUpdateRequest request) {
        Set<String> uniqueExternalVoiceIds = new LinkedHashSet<>(request.getExternalVoiceIds());
        if (uniqueExternalVoiceIds.size() != request.getExternalVoiceIds().size()) {
            throw ApiException.error(ErrorCode.INVALID_REQUEST, "Duplicate external voice ids are not allowed");
        }

        VoiceFolder folder = resolveOwnedFolder(request.getFolderId(), user.getId());
        List<VoiceOwnership> voiceOwnerships = voiceOwnershipRepository.findAllByUserIdAndVoiceAssetExternalVoiceIdIn(
                user.getId(),
                uniqueExternalVoiceIds
        );

        if (voiceOwnerships.size() != uniqueExternalVoiceIds.size()) {
            throw ApiException.error(ErrorCode.INVALID_REQUEST, "Some external voice ids were not found");
        }

        voiceOwnerships.forEach(voiceOwnership -> voiceOwnership.changeFolder(folder));

        return voiceOwnerships.stream()
                .map(OwnedVoiceAssetResponse::from)
                .toList();
    }

    @Transactional
    public CreateClonedVoiceAssetResponse createClonedVoice(
            User user,
            MultipartFile file,
            String name,
            String description
    ) {
        validateCreateClonedVoiceRequest(file, name);

        String trimmedName = name.trim();
        String trimmedDescription = StringUtils.hasText(description) ? description.trim() : null;

        SupertoneCreateClonedVoiceResponse response = supertoneVoiceClient.createClonedVoice(
                file,
                trimmedName,
                trimmedDescription
        );

        if (voiceAssetRepository.existsById(response.voiceId())) {
            throw ApiException.error(ErrorCode.INVALID_REQUEST, "Voice already exists for external voice id: " + response.voiceId());
        }

        VoiceAsset voiceAsset = VoiceAsset.createCloned(user, trimmedName, response.voiceId());
        VoiceAsset savedVoiceAsset = voiceAssetRepository.save(voiceAsset);

        VoiceOwnership voiceOwnership = VoiceOwnership.createCreatedOwnership(user, savedVoiceAsset);

        voiceOwnershipRepository.save(voiceOwnership);

        return CreateClonedVoiceAssetResponse.from(voiceAsset);
    }

    private void validateCreateClonedVoiceRequest(MultipartFile file, String name) {
        if (file == null || file.isEmpty()) {
            throw ApiException.error(ErrorCode.INVALID_REQUEST, "Audio file is required");
        }
        if (!StringUtils.hasText(name)) {
            throw ApiException.error(ErrorCode.INVALID_REQUEST, "Voice name is required");
        }
        if (name.trim().length() > 100) {
            throw ApiException.error(ErrorCode.INVALID_REQUEST, "Voice name must be 100 characters or fewer");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw ApiException.error(ErrorCode.SUPERTONE_FILE_TOO_LARGE);
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            throw ApiException.error(ErrorCode.SUPERTONE_UNSUPPORTED_MEDIA_TYPE);
        }

        String lowerCaseFileName = originalFilename.toLowerCase(Locale.ROOT);
        if (!(lowerCaseFileName.endsWith(".wav") || lowerCaseFileName.endsWith(".mp3"))) {
            throw ApiException.error(ErrorCode.SUPERTONE_UNSUPPORTED_MEDIA_TYPE);
        }
    }

    private void validateOwnedFolder(Long folderId, Long userId) {
        resolveOwnedFolder(folderId, userId);
    }

    private VoiceFolder resolveOwnedFolder(Long folderId, Long userId) {
        if (folderId == null) {
            return null;
        }

        return voiceFolderRepository.findByIdAndOwnerId(folderId, userId)
                .orElseThrow(() -> ApiException.error(ErrorCode.VOICE_FOLDER_NOT_FOUND));
    }
}
