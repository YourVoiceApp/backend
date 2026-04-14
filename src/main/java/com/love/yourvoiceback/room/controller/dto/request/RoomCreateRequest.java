package com.love.yourvoiceback.room.controller.dto.request;

import com.love.yourvoiceback.room.enums.JoinPolicy;
import jakarta.validation.constraints.NotBlank;
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
    private String title;
    private JoinPolicy joinPolicy;
    private Long maxParticipants;
    private String password;

}
