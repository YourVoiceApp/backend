package com.love.yourvoiceback.voice.controller;

import com.love.yourvoiceback.common.security.CurrentUser;
import com.love.yourvoiceback.user.User;
import com.love.yourvoiceback.voice.dto.request.VoiceOwnershipFolderUpdateRequest;
import com.love.yourvoiceback.voice.dto.request.VoiceTextToSpeechRequest;
import com.love.yourvoiceback.voice.dto.response.CreateClonedVoiceAssetResponse;
import com.love.yourvoiceback.voice.dto.response.OwnedVoiceAssetResponse;
import com.love.yourvoiceback.voice.dto.response.VoiceTextToSpeechResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.love.yourvoiceback.voice.service.VoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @PostMapping("/{ownershipId}/text-to-speech")
    @Operation(summary = "선택한 내 음성과 텍스트를 이용해 TTS 오디오를 생성하고 재생/다운로드 URL을 반환합니다.")
    public ResponseEntity<VoiceTextToSpeechResponse> createSpeech(
            @PathVariable Long ownershipId,
            @Valid @RequestBody VoiceTextToSpeechRequest request,
            @CurrentUser User user
    ) {
        return ResponseEntity.ok(voiceService.createSpeech(user, ownershipId, request));
    }

    @DeleteMapping("/{ownershipId}")
    @Operation(summary = "현재 로그인한 사용자의 음성 소유 항목을 삭제합니다.")
    public ResponseEntity<Void> deleteOwnedVoice(
            @PathVariable Long ownershipId,
            @CurrentUser User user
    ) {
        voiceService.deleteOwnedVoice(user, ownershipId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(
            value = "/generated-audios/{generatedAudioId}/stream",
            produces = "audio/mpeg"
    )
    @Operation(summary = "생성된 TTS 오디오를 앱에서 바로 재생할 수 있도록 스트리밍합니다.")
    @ApiResponse(
            responseCode = "200",
            description = "Binary mp3 audio stream",
            content = @Content(mediaType = "audio/mpeg", schema = @Schema(type = "string", format = "binary"))
    )
    public ResponseEntity<byte[]> streamGeneratedAudio(
            @PathVariable Long generatedAudioId,
            @CurrentUser User user
    ) {
        var response = voiceService.getGeneratedAudioForStream(user, generatedAudioId);
        return ResponseEntity.ok()
                .contentType(response.contentType())
                .contentLength(response.body().length)
                .body(response.body());
    }

    @GetMapping(
            value = "/generated-audios/{generatedAudioId}/download",
            produces = "audio/mpeg"
    )
    @Operation(summary = "생성된 TTS 오디오를 다운로드합니다.")
    @ApiResponse(
            responseCode = "200",
            description = "Binary mp3 audio download",
            content = @Content(mediaType = "audio/mpeg", schema = @Schema(type = "string", format = "binary"))
    )
    public ResponseEntity<byte[]> downloadGeneratedAudio(
            @PathVariable Long generatedAudioId,
            @CurrentUser User user
    ) {
        var response = voiceService.getGeneratedAudioForDownload(user, generatedAudioId);
        return ResponseEntity.ok()
                .contentType(response.contentType())
                .contentLength(response.body().length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(response.filename()).build().toString()
                )
                .body(response.body());
    }
}
