package com.love.yourvoiceback.voice.controller;

import com.love.yourvoiceback.common.security.CurrentUser;
import com.love.yourvoiceback.user.User;
import com.love.yourvoiceback.voice.dto.request.VoiceFolderCreateRequest;
import com.love.yourvoiceback.voice.dto.request.VoiceFolderUpdateRequest;
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

import java.util.List;

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

    @GetMapping
    @Operation(summary = "내 음성 폴더 목록을 조회합니다.")
    public ResponseEntity<List<VoiceFolderResponse>> getFolders(
            @RequestParam(required = false) Long parentId,
            @CurrentUser User user
    ) {
        return ResponseEntity.ok(voiceFolderService.getFolders(parentId, user));
    }

    @GetMapping("/{folderId}")
    @Operation(summary = "내 음성 폴더 정보를 조회합니다.")
    public ResponseEntity<VoiceFolderResponse> getFolder(
            @PathVariable Long folderId,
            @CurrentUser User user
    ) {
        return ResponseEntity.ok(voiceFolderService.getFolder(folderId, user));
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
