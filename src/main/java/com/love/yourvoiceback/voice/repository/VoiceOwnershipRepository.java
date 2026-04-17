package com.love.yourvoiceback.voice.repository;

import com.love.yourvoiceback.voice.domain.VoiceOwnership;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    long countByUserIdAndFolderId(Long userId, Long folderId);

    @Query("""
            select o.folder.id as folderId, count(o.id) as totalCount
            from VoiceOwnership o
            where o.user.id = :userId
              and o.folder.id in :folderIds
            group by o.folder.id
            """)
    List<FolderVoiceCountProjection> countVoicesByUserIdAndFolderIds(
            @Param("userId") Long userId,
            @Param("folderIds") Collection<Long> folderIds
    );

    @EntityGraph(attributePaths = {"voiceAsset", "folder"})
    Optional<VoiceOwnership> findByIdAndUserId(Long id, Long userId);

    interface FolderVoiceCountProjection {
        Long getFolderId();

        long getTotalCount();
    }
}
