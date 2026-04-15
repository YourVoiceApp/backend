package com.love.yourvoiceback.voice.service;

import com.love.yourvoiceback.common.exception.ApiException;
import com.love.yourvoiceback.common.exception.ErrorCode;
import com.love.yourvoiceback.training.VoiceTrainingJob;
import com.love.yourvoiceback.training.VoiceTrainingJobRepository;
import com.love.yourvoiceback.user.User;
import com.love.yourvoiceback.voice.domain.VoiceFolder;
import com.love.yourvoiceback.voice.domain.VoiceOwnership;
import com.love.yourvoiceback.voice.dto.request.VoiceFolderCreateRequest;
import com.love.yourvoiceback.voice.dto.request.VoiceFolderUpdateRequest;
import com.love.yourvoiceback.voice.dto.response.VoiceFolderResponse;
import com.love.yourvoiceback.voice.repository.VoiceFolderRepository;
import com.love.yourvoiceback.voice.repository.VoiceOwnershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoiceFolderService {

    private final VoiceFolderRepository voiceFolderRepository;
    private final VoiceOwnershipRepository voiceOwnershipRepository;
    private final VoiceTrainingJobRepository voiceTrainingJobRepository;

    @Transactional
    public VoiceFolderResponse createFolder(VoiceFolderCreateRequest request, User user) {
        String folderName = request.getName();
        VoiceFolder parentFolder = resolveParentFolder(request.getParentFolderId(), user.getId());

        validateDuplicateFolderName(user.getId(), parentFolder, folderName, null);

        VoiceFolder voiceFolder = VoiceFolder.of(user, parentFolder, folderName);

        return VoiceFolderResponse.from(voiceFolderRepository.save(voiceFolder));
    }

    @Transactional(readOnly = true)
    public List<VoiceFolderResponse> getFolders(Long parentFolderId, User user) {
        if (parentFolderId != null) {
            getOwnedFolder(parentFolderId, user.getId());
            return voiceFolderRepository.findAllByOwnerIdAndParentIdOrderByCreatedAtAsc(user.getId(), parentFolderId).stream()
                    .map(VoiceFolderResponse::from)
                    .toList();
        }

        return voiceFolderRepository.findAllByOwnerIdAndParentIsNullOrderByCreatedAtAsc(user.getId()).stream()
                .map(VoiceFolderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public VoiceFolderResponse getFolder(Long folderId, User user) {
        return VoiceFolderResponse.from(getOwnedFolder(folderId, user.getId()));
    }

    @Transactional
    public VoiceFolderResponse updateFolder(Long folderId, VoiceFolderUpdateRequest request, User user) {
        VoiceFolder voiceFolder = getOwnedFolder(folderId, user.getId());

        String folderName = request.getName();
        VoiceFolder parentFolder = resolveParentFolder(request.getParentFolderId(), user.getId());

        validateFolderHierarchy(voiceFolder, parentFolder);
        validateDuplicateFolderName(user.getId(), parentFolder, folderName, voiceFolder.getId());

        voiceFolder.updateFolder(folderName, parentFolder);

        return VoiceFolderResponse.from(voiceFolder);
    }

    @Transactional
    public void deleteFolder(Long folderId, User user) {
        VoiceFolder voiceFolder = getOwnedFolder(folderId, user.getId());
        List<VoiceFolder> foldersToDelete = collectFoldersForDeletion(voiceFolder, user.getId());
        List<Long> folderIds = foldersToDelete.stream()
                .map(VoiceFolder::getId)
                .collect(Collectors.toList());

        detachVoiceOwnerships(user.getId(), folderIds);
        detachTrainingJobs(folderIds);

        voiceFolderRepository.deleteAll(foldersToDelete);
    }

    private VoiceFolder getOwnedFolder(Long folderId, Long ownerId) {
        return voiceFolderRepository.findByIdAndOwnerId(folderId, ownerId)
                .orElseThrow(() -> ApiException.error(ErrorCode.VOICE_FOLDER_NOT_FOUND));
    }

    private VoiceFolder resolveParentFolder(Long parentFolderId, Long ownerId) {
        if (parentFolderId == null) {
            return null;
        }
        return getOwnedFolder(parentFolderId, ownerId);
    }

    private void validateFolderHierarchy(VoiceFolder voiceFolder, VoiceFolder parentFolder) {
        if (parentFolder == null) {
            return;
        }
        if (voiceFolder.getId().equals(parentFolder.getId())) {
            throw ApiException.error(ErrorCode.INVALID_REQUEST, "A folder cannot be its own parent");
        }

        VoiceFolder current = parentFolder;
        while (current != null) {
            if (voiceFolder.getId().equals(current.getId())) {
                throw ApiException.error(ErrorCode.INVALID_REQUEST, "A folder cannot be moved into its descendant");
            }
            current = current.getParent();
        }
    }

    private void validateDuplicateFolderName(Long ownerId, VoiceFolder parentFolder, String folderName, Long folderIdToExclude) {
        boolean exists = parentFolder == null
                ? hasDuplicateRootFolderName(ownerId, folderName, folderIdToExclude)
                : hasDuplicateChildFolderName(ownerId, parentFolder.getId(), folderName, folderIdToExclude);

        if (exists) {
            throw ApiException.error(ErrorCode.INVALID_REQUEST, "A folder with the same name already exists in this location");
        }
    }

    private boolean hasDuplicateRootFolderName(Long ownerId, String folderName, Long folderIdToExclude) {
        if (folderIdToExclude == null) {
            return voiceFolderRepository.existsByOwnerIdAndParentIsNullAndName(ownerId, folderName);
        }
        return voiceFolderRepository.existsByOwnerIdAndParentIsNullAndNameAndIdNot(ownerId, folderName, folderIdToExclude);
    }

    private boolean hasDuplicateChildFolderName(Long ownerId, Long parentFolderId, String folderName, Long folderIdToExclude) {
        if (folderIdToExclude == null) {
            return voiceFolderRepository.existsByOwnerIdAndParentIdAndName(ownerId, parentFolderId, folderName);
        }
        return voiceFolderRepository.existsByOwnerIdAndParentIdAndNameAndIdNot(ownerId, parentFolderId, folderName, folderIdToExclude);
    }

    private List<VoiceFolder> collectFoldersForDeletion(VoiceFolder folder, Long ownerId) {
        List<VoiceFolder> folders = voiceFolderRepository.findAllByOwnerIdAndParentIdOrderByCreatedAtAsc(ownerId, folder.getId()).stream()
                .flatMap(childFolder -> collectFoldersForDeletion(childFolder, ownerId).stream())
                .collect(Collectors.toList());
        folders.add(folder);
        return folders;
    }

    private void detachVoiceOwnerships(Long userId, List<Long> folderIds) {
        List<VoiceOwnership> voiceOwnerships = voiceOwnershipRepository.findAllByUserIdAndFolderIdIn(userId, folderIds);
        voiceOwnerships.forEach(VoiceOwnership::clearFolder);
        voiceOwnershipRepository.saveAll(voiceOwnerships);
    }

    private void detachTrainingJobs(List<Long> folderIds) {
        List<VoiceTrainingJob> trainingJobs = voiceTrainingJobRepository.findAllByRequestedFolderIdIn(folderIds);
        trainingJobs.forEach(VoiceTrainingJob::clearRequestedFolder);
        voiceTrainingJobRepository.saveAll(trainingJobs);
    }
}
