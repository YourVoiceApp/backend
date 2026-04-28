package com.love.yourvoiceback.auth.controller;

import com.love.yourvoiceback.auth.dto.AuthResponse;
import com.love.yourvoiceback.auth.dto.SignupRequest;
import com.love.yourvoiceback.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnProperty(name = "app.test-auth.enabled", havingValue = "true")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/test")
public class TestController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "app.test-auth.enabled=true 일 때만 노출. 이메일 인증 없이 회원가입합니다 (로컬·스테이징 등).")
    public ResponseEntity<AuthResponse> signupWithoutEmailVerification(
            @Valid @RequestBody SignupRequest request
    ) {
        return ResponseEntity.ok(authService.signupWithoutEmailVerification(request));
    }
}
