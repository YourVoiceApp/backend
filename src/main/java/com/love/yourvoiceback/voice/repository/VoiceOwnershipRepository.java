package com.love.yourvoiceback.voice.repository;

import com.love.yourvoiceback.voice.domain.VoiceOwnership;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface VoiceOwnershipRepository extends JpaRepository<VoiceOwnership, Long> {
    @EntityGraph(attributePaths = {"voiceAsset", "folder"})
    List<VoiceOwnership> findAllByUserIdOrderByAcquiredAtDesc(Long userId);

    @EntityGraph(attributePaths = {"voiceAsset", "folder"})
    List<VoiceOwnership> findAllByUserIdAndFolderIdOrderByAcquiredAtDesc(Long userId, Long folderId);

    @EntityGraph(attributePaths = {"voiceAsset", "folder"})
    List<VoiceOwnership> findAllByUserIdAndFolderIsNullOrderByAcquiredAtDesc(Long userId);

    @EntityGraph(attributePaths = {"voiceAsset", "folder"})
    List<VoiceOwnership> findAllByUserIdAndVoiceAssetExternalVoiceIdIn(Long userId, Collection<String> externalVoiceIds);

    List<VoiceOwnership> findAllByUserIdAndFolderIdIn(Long userId, Collection<Long> folderIds);
}
