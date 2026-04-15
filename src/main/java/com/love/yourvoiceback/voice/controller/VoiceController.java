package com.love.yourvoiceback.voice.controller;

import com.love.yourvoiceback.common.security.CurrentUser;
import com.love.yourvoiceback.user.User;
import com.love.yourvoiceback.voice.dto.request.VoiceOwnershipFolderUpdateRequest;
import com.love.yourvoiceback.voice.dto.response.CreateClonedVoiceAssetResponse;
import com.love.yourvoiceback.voice.dto.response.OwnedVoiceAssetResponse;
import com.love.yourvoiceback.voice.service.VoiceService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/voices")
public class VoiceController {

    private final VoiceService voiceService;

    @GetMapping
    @Operation(summary = "현재 로그인한 사용자가 소유한 음성 목록을 조회합니다.")
    public ResponseEntity<List<OwnedVoiceAssetResponse>> getOwnedVoices(
            @RequestParam(required = false) Long folderId,
            @CurrentUser User user
    ) {
        return ResponseEntity.ok(voiceService.getOwnedVoices(user, folderId));
    }

    @GetMapping("/unassigned")
    @Operation(summary = "현재 로그인한 사용자의 미분류 음성 목록을 조회합니다.")
    public ResponseEntity<List<OwnedVoiceAssetResponse>> getUnassignedOwnedVoices(@CurrentUser User user) {
        return ResponseEntity.ok(voiceService.getUnassignedOwnedVoices(user));
    }

    @PatchMapping("/folder")
    @Operation(summary = "내 음성을 특정 폴더로 이동하거나 미분류로 변경합니다.")
    public ResponseEntity<List<OwnedVoiceAssetResponse>> updateOwnedVoiceFolder(
            @Valid @RequestBody VoiceOwnershipFolderUpdateRequest request,
            @CurrentUser User user
    ) {
        return ResponseEntity.ok(voiceService.updateOwnedVoiceFolder(user, request));
    }

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
