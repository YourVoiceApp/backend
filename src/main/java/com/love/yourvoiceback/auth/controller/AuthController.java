package com.love.yourvoiceback.auth.controller;

import com.love.yourvoiceback.auth.dto.AuthResponse;
import com.love.yourvoiceback.auth.dto.GoogleLoginRequest;
import com.love.yourvoiceback.auth.dto.KakaoLoginRequest;
import com.love.yourvoiceback.auth.dto.LoginRequest;
import com.love.yourvoiceback.auth.dto.SendEmailVerificationRequest;
import com.love.yourvoiceback.auth.dto.SignupRequest;
import com.love.yourvoiceback.auth.dto.TokenRefreshRequest;
import com.love.yourvoiceback.auth.dto.VerifyEmailRequest;
import com.love.yourvoiceback.auth.service.AuthService;
import com.love.yourvoiceback.auth.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/email/send-verification")
    @Operation(summary = "회원가입용 이메일 인증 코드를 발송합니다.")
    public ResponseEntity<Void> sendVerification(@Valid @RequestBody SendEmailVerificationRequest request) {
        emailVerificationService.sendVerificationCode(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email/verify")
    @Operation(summary = "이메일 인증 코드를 검증합니다.")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.verifyCode(request.email(), request.code());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/signup")
    @Operation(summary = "이메일 회원가입 후 토큰을 발급합니다.")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/login")
    @Operation(summary = "이메일과 비밀번호로 로그인합니다.")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/google")
    @Operation(summary = "구글 계정으로 로그인하고 토큰을 발급합니다.")
    public ResponseEntity<AuthResponse> google(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithGoogle(request));
    }

    @PostMapping("/kakao")
    @Operation(summary = "카카오 계정으로 로그인하고 토큰을 발급합니다.")
    public ResponseEntity<AuthResponse> kakao(@Valid @RequestBody KakaoLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithKakao(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "리프레시 토큰으로 액세스 토큰을 재발급합니다.")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "리프레시 토큰을 만료 처리해 로그아웃합니다.")
    public ResponseEntity<Void> logout(@Valid @RequestBody TokenRefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
