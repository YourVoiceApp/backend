package com.love.yourvoiceback.voice.dto.response;

import com.love.yourvoiceback.voice.domain.VoiceFolder;

import java.time.LocalDateTime;

public record VoiceFolderResponse(
        Long id,
        Long parentFolderId,
        String name,
        long voiceCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static VoiceFolderResponse from(VoiceFolder voiceFolder, long voiceCount) {
        return new VoiceFolderResponse(
                voiceFolder.getId(),
                voiceFolder.getParent() != null ? voiceFolder.getParent().getId() : null,
                voiceFolder.getName(),
                voiceCount,
                voiceFolder.getCreatedAt(),
                voiceFolder.getUpdatedAt()
        );
    }
}
