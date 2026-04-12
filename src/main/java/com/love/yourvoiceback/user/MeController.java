package com.love.yourvoiceback.user;

import com.love.yourvoiceback.user.dto.ChangePasswordRequest;
import com.love.yourvoiceback.user.dto.MeResponse;
import com.love.yourvoiceback.user.dto.SocialAccountResponse;
import com.love.yourvoiceback.user.dto.UpdateProfileRequest;
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
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        return ResponseEntity.ok(userService.getMe(currentUserId(authentication)));
    }

    @PatchMapping("/profile")
    public ResponseEntity<MeResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(userService.updateProfile(currentUserId(authentication), request));
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(currentUserId(authentication), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/social-accounts")
    public ResponseEntity<List<SocialAccountResponse>> socialAccounts(Authentication authentication) {
        return ResponseEntity.ok(userService.getSocialAccounts(currentUserId(authentication)));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteMe(Authentication authentication) {
        userService.deleteMe(currentUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    private Long currentUserId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
