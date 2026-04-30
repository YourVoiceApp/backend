package com.love.yourvoiceback.voice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RenameOwnedVoiceRequest {

    @NotBlank(message = "Voice name is required")
    @Size(max = 100, message = "Voice name must be 100 characters or fewer")
    private String name;
}
