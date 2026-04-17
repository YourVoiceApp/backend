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
import com.love.yourvoiceback.voice.dto.response.OwnedVoiceAssetResponse;
import com.love.yourvoiceback.voice.dto.response.VoiceFolderContentsResponse;
import com.love.yourvoiceback.voice.dto.response.VoiceFolderResponse;
import com.love.yourvoiceback.voice.repository.VoiceFolderRepository;
import com.love.yourvoiceback.voice.repository.VoiceOwnershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        return buildFolderResponse(voiceFolderRepository.save(voiceFolder), user.getId());
    }

    @Transactional(readOnly = true)
    public VoiceFolderContentsResponse getFolderContents(Long parentFolderId, User user) {
        List<VoiceFolderResponse> folders = getChildFolderResponses(parentFolderId, user.getId());
        List<OwnedVoiceAssetResponse> voices;

        if (parentFolderId == null) {
            voices = voiceOwnershipRepository.findAllByUserIdAndFolderIsNullOrderByAcquiredAtDesc(user.getId()).stream()
                    .map(OwnedVoiceAssetResponse::from)
                    .toList();
        } else {
            voices = voiceOwnershipRepository.findAllByUserIdAndFolderIdOrderByAcquiredAtDesc(user.getId(), parentFolderId).stream()
                    .map(OwnedVoiceAssetResponse::from)
                    .toList();
        }

        return VoiceFolderContentsResponse.builder()
                .totalVoiceCount(calculateTotalVoiceCount(folders, voices))
                .folders(folders)
                .voices(voices)
                .build();
    }

    @Transactional
    public VoiceFolderResponse updateFolder(Long folderId, VoiceFolderUpdateRequest request, User user) {
        VoiceFolder voiceFolder = getOwnedFolder(folderId, user.getId());

        String folderName = request.getName();
        VoiceFolder parentFolder = resolveParentFolder(request.getParentFolderId(), user.getId());

        validateFolderHierarchy(voiceFolder, parentFolder);
        validateDuplicateFolderName(user.getId(), parentFolder, folderName, voiceFolder.getId());

        voiceFolder.updateFolder(folderName, parentFolder);

        return buildFolderResponse(voiceFolder, user.getId());
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

    private List<VoiceFolderResponse> buildFolderResponses(List<VoiceFolder> folders, Long userId) {
        if (folders.isEmpty()) {
            return List.of();
        }

        FolderVoiceCountContext countContext = buildFolderVoiceCountContext(userId);

        return folders.stream()
                .map(folder -> VoiceFolderResponse.from(
                        folder,
                        getTotalVoiceCount(folder.getId(), countContext)
                ))
                .toList();
    }

    private List<VoiceFolderResponse> getChildFolderResponses(Long parentFolderId, Long userId) {
        if (parentFolderId != null) {
            getOwnedFolder(parentFolderId, userId);
            return buildFolderResponses(
                    voiceFolderRepository.findAllByOwnerIdAndParentIdOrderByCreatedAtAsc(userId, parentFolderId),
                    userId
            );
        }

        return buildFolderResponses(
                voiceFolderRepository.findAllByOwnerIdAndParentIsNullOrderByCreatedAtAsc(userId),
                userId
        );
    }

    private VoiceFolderResponse buildFolderResponse(VoiceFolder folder, Long userId) {
        FolderVoiceCountContext countContext = buildFolderVoiceCountContext(userId);
        return VoiceFolderResponse.from(folder, getTotalVoiceCount(folder.getId(), countContext));
    }

    private FolderVoiceCountContext buildFolderVoiceCountContext(Long userId) {
        List<VoiceFolder> allFolders = voiceFolderRepository.findAllByOwnerIdOrderByCreatedAtAsc(userId);
        List<Long> folderIds = allFolders.stream()
                .map(VoiceFolder::getId)
                .toList();

        Map<Long, Long> voiceCounts = getVoiceCounts(userId, folderIds);
        Map<Long, List<Long>> childFolderIdsByParentId = new HashMap<>();

        for (VoiceFolder voiceFolder : allFolders) {
            if (voiceFolder.getParent() == null) {
                continue;
            }

            childFolderIdsByParentId
                    .computeIfAbsent(voiceFolder.getParent().getId(), ignored -> new ArrayList<>())
                    .add(voiceFolder.getId());
        }

        return new FolderVoiceCountContext(voiceCounts, childFolderIdsByParentId, new HashMap<>());
    }

    private Map<Long, Long> getVoiceCounts(Long userId, Collection<Long> folderIds) {
        if (folderIds.isEmpty()) {
            return Map.of();
        }

        return voiceOwnershipRepository.countVoicesByUserIdAndFolderIds(userId, folderIds).stream()
                .collect(Collectors.toMap(
                        VoiceOwnershipRepository.FolderVoiceCountProjection::getFolderId,
                        VoiceOwnershipRepository.FolderVoiceCountProjection::getTotalCount
                ));
    }

    private long getTotalVoiceCount(Long folderId, FolderVoiceCountContext countContext) {
        Long cachedCount = countContext.totalVoiceCounts().get(folderId);
        if (cachedCount != null) {
            return cachedCount;
        }

        long totalCount = countContext.directVoiceCounts().getOrDefault(folderId, 0L);
        for (Long childFolderId : countContext.childFolderIdsByParentId().getOrDefault(folderId, List.of())) {
            totalCount += getTotalVoiceCount(childFolderId, countContext);
        }

        countContext.totalVoiceCounts().put(folderId, totalCount);
        return totalCount;
    }

    private long calculateTotalVoiceCount(List<VoiceFolderResponse> folders, List<OwnedVoiceAssetResponse> voices) {
        return folders.stream()
                .mapToLong(VoiceFolderResponse::voiceCount)
                .sum() + voices.size();
    }

    private record FolderVoiceCountContext(
            Map<Long, Long> directVoiceCounts,
            Map<Long, List<Long>> childFolderIdsByParentId,
            Map<Long, Long> totalVoiceCounts
    ) {
    }
}
