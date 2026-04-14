package com.love.yourvoiceback.auth.controller;

import com.love.yourvoiceback.auth.dto.AuthResponse;
import com.love.yourvoiceback.auth.dto.SignupRequest;
import com.love.yourvoiceback.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("local")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/test")
public class TestController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "로컬 개발용으로 이메일 인증 없이 회원가입합니다.")
    public ResponseEntity<AuthResponse> signupWithoutEmailVerification(
            @Valid @RequestBody SignupRequest request
    ) {
        return ResponseEntity.ok(authService.signupWithoutEmailVerification(request));
    }
}
