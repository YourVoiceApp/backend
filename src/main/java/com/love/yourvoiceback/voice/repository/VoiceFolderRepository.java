package com.love.yourvoiceback.voice.repository;

import com.love.yourvoiceback.voice.domain.VoiceFolder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoiceFolderRepository extends JpaRepository<VoiceFolder, Long> {
    Optional<VoiceFolder> findByIdAndOwnerId(Long id, Long ownerId);

    List<VoiceFolder> findAllByOwnerIdOrderByCreatedAtAsc(Long ownerId);

    List<VoiceFolder> findAllByOwnerIdAndParentIsNullOrderByCreatedAtAsc(Long ownerId);

    List<VoiceFolder> findAllByOwnerIdAndParentIdOrderByCreatedAtAsc(Long ownerId, Long parentId);

    boolean existsByOwnerIdAndParentIsNullAndName(Long ownerId, String name);

    boolean existsByOwnerIdAndParentIdAndName(Long ownerId, Long parentId, String name);

    boolean existsByOwnerIdAndParentIsNullAndNameAndIdNot(Long ownerId, String name, Long id);

    boolean existsByOwnerIdAndParentIdAndNameAndIdNot(Long ownerId, Long parentId, String name, Long id);

    boolean existsByParentId(Long parentId);
}
