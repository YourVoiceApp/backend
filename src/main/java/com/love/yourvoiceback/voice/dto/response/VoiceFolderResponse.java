package com.love.yourvoiceback.voice.dto.response;

import com.love.yourvoiceback.voice.domain.VoiceFolder;

import java.time.LocalDateTime;

public record VoiceFolderResponse(
        Long id,
        Long parentFolderId,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static VoiceFolderResponse from(VoiceFolder voiceFolder) {
        return new VoiceFolderResponse(
                voiceFolder.getId(),
                voiceFolder.getParent() != null ? voiceFolder.getParent().getId() : null,
                voiceFolder.getName(),
                voiceFolder.getCreatedAt(),
                voiceFolder.getUpdatedAt()
        );
    }
}
