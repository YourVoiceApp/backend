package com.love.yourvoiceback.training;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface VoiceTrainingJobRepository extends JpaRepository<VoiceTrainingJob, Long> {
    List<VoiceTrainingJob> findAllByRequestedFolderIdIn(Collection<Long> folderIds);
}
