package com.love.yourvoiceback.voice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VoiceFolderUpdateRequest {
    @NotBlank
    @Size(max = 100)
    private String name;

    private Long parentFolderId;
}
