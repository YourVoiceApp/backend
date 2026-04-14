package com.love.yourvoiceback.voice.controller;

import com.love.yourvoiceback.common.security.CurrentUser;
import com.love.yourvoiceback.user.User;
import com.love.yourvoiceback.voice.dto.CreateClonedVoiceAssetResponse;
import com.love.yourvoiceback.voice.service.VoiceService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/voices")
public class VoiceController {

    private final VoiceService voiceService;

    @PostMapping(path = "/cloned-voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "학습용 음성 파일을 업로드해 Supertone 클론 보이스를 생성합니다.")
    public ResponseEntity<CreateClonedVoiceAssetResponse> createClonedVoice(
            @CurrentUser User user,
            @RequestPart("files") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description
    ) {
        return ResponseEntity.ok(voiceService.createClonedVoice(user, file, name, description));
    }
}
