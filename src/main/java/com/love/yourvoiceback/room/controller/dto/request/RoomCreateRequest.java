package com.love.yourvoiceback.room.controller.dto.request;

import com.love.yourvoiceback.room.enums.JoinPolicy;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoomCreateRequest {
    @NotBlank
    @Size(max = 100)
    private String title;

    @NotNull
    private JoinPolicy joinPolicy;

    @NotNull
    @Min(1)
    private Long maxParticipants;

    private String password;

}
