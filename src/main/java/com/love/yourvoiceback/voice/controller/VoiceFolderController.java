package com.love.yourvoiceback.voice.controller;

import com.love.yourvoiceback.common.security.CurrentUser;
import com.love.yourvoiceback.user.User;
import com.love.yourvoiceback.voice.dto.request.VoiceFolderCreateRequest;
import com.love.yourvoiceback.voice.dto.request.VoiceFolderUpdateRequest;
import com.love.yourvoiceback.voice.dto.response.VoiceFolderContentsResponse;
import com.love.yourvoiceback.voice.dto.response.VoiceFolderResponse;
import com.love.yourvoiceback.voice.service.VoiceFolderService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/voice-folders")
public class VoiceFolderController {

    private final VoiceFolderService voiceFolderService;

    @PostMapping
    @Operation(summary = "내 음성 폴더를 생성합니다.")
    public ResponseEntity<VoiceFolderResponse> createFolder(
            @Valid @RequestBody VoiceFolderCreateRequest request,
            @CurrentUser User user
    ) {
        return ResponseEntity.ok(voiceFolderService.createFolder(request, user));
    }

    @GetMapping("/contents")
    @Operation(summary = "특정 폴더 화면에 필요한 하위 폴더와 음성 파일 목록을 함께 조회합니다.")
    public ResponseEntity<VoiceFolderContentsResponse> getFolderContents(
            @RequestParam(required = false) Long parentId,
            @CurrentUser User user
    ) {
        return ResponseEntity.ok(voiceFolderService.getFolderContents(parentId, user));
    }

    @PutMapping("/{folderId}")
    @Operation(summary = "내 음성 폴더 정보를 수정합니다.")
    public ResponseEntity<VoiceFolderResponse> updateFolder(
            @PathVariable Long folderId,
            @Valid @RequestBody VoiceFolderUpdateRequest request,
            @CurrentUser User user
    ) {
        return ResponseEntity.ok(voiceFolderService.updateFolder(folderId, request, user));
    }

    @DeleteMapping("/{folderId}")
    @Operation(summary = "내 음성 폴더를 삭제합니다.")
    public ResponseEntity<Void> deleteFolder(
            @PathVariable Long folderId,
            @CurrentUser User user
    ) {
        voiceFolderService.deleteFolder(folderId, user);
        return ResponseEntity.noContent().build();
    }
}
