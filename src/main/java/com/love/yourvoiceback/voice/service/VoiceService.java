package com.love.yourvoiceback.voice.service;

import com.love.yourvoiceback.common.exception.ApiException;
import com.love.yourvoiceback.common.exception.ErrorCode;
import com.love.yourvoiceback.external.supertone.SupertoneVoiceClient;
import com.love.yourvoiceback.external.supertone.dto.SupertoneCreateClonedVoiceResponse;
import com.love.yourvoiceback.user.User;
import com.love.yourvoiceback.voice.VoiceAsset;
import com.love.yourvoiceback.voice.VoiceAssetRepository;
import com.love.yourvoiceback.voice.VoiceOwnership;
import com.love.yourvoiceback.voice.VoiceOwnershipRepository;
import com.love.yourvoiceback.voice.dto.CreateClonedVoiceAssetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class VoiceService {

    private static final long MAX_FILE_SIZE_BYTES = 3L * 1024L * 1024L;

    private final SupertoneVoiceClient supertoneVoiceClient;
    private final VoiceAssetRepository voiceAssetRepository;
    private final VoiceOwnershipRepository voiceOwnershipRepository;

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

        if (voiceAssetRepository.existsByExternalVoiceId(response.voiceId())) {
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
}
