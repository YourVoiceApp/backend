package com.love.yourvoiceback.user;

import com.love.yourvoiceback.user.dto.ChangePasswordRequest;
import com.love.yourvoiceback.user.dto.MeResponse;
import com.love.yourvoiceback.user.dto.SocialAccountResponse;
import com.love.yourvoiceback.user.dto.UpdateProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/me")
public class MeController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "현재 로그인한 사용자 정보를 조회합니다.")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        return ResponseEntity.ok(userService.getMe(currentUserId(authentication)));
    }

    @PatchMapping("/profile")
    @Operation(summary = "현재 로그인한 사용자의 프로필을 수정합니다.")
    public ResponseEntity<MeResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(userService.updateProfile(currentUserId(authentication), request));
    }

    @PatchMapping("/password")
    @Operation(summary = "현재 로그인한 사용자의 비밀번호를 변경합니다.")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(currentUserId(authentication), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/social-accounts")
    @Operation(summary = "연결된 소셜 로그인 계정 목록을 조회합니다.")
    public ResponseEntity<List<SocialAccountResponse>> socialAccounts(Authentication authentication) {
        return ResponseEntity.ok(userService.getSocialAccounts(currentUserId(authentication)));
    }

    @DeleteMapping
    @Operation(summary = "현재 로그인한 사용자 계정을 삭제합니다.")
    public ResponseEntity<Void> deleteMe(Authentication authentication) {
        userService.deleteMe(currentUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    private Long currentUserId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
