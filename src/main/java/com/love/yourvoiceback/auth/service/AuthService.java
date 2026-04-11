package com.love.yourvoiceback.auth.service;

import com.love.yourvoiceback.auth.domain.RefreshToken;
import com.love.yourvoiceback.auth.domain.SocialProvider;
import com.love.yourvoiceback.auth.domain.UserSocialAccount;
import com.love.yourvoiceback.auth.dto.AuthResponse;
import com.love.yourvoiceback.auth.dto.GoogleLoginRequest;
import com.love.yourvoiceback.auth.google.GoogleIdTokenPayload;
import com.love.yourvoiceback.auth.google.GoogleTokenVerifier;
import com.love.yourvoiceback.auth.jwt.JwtTokenProvider;
import com.love.yourvoiceback.auth.repository.RefreshTokenRepository;
import com.love.yourvoiceback.auth.repository.UserSocialAccountRepository;
import com.love.yourvoiceback.auth.util.TokenHashUtil;
import com.love.yourvoiceback.user.User;
import com.love.yourvoiceback.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserSocialAccountRepository userSocialAccountRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${auth.jwt.refresh-expiration-seconds}")
    private long refreshExpirationSeconds;

    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleIdTokenPayload payload = googleTokenVerifier.verify(request.idToken());
        User user = findOrCreateUserByGooglePayload(payload);

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHashUtil.sha256(refreshToken))
                .deviceInfo(request.deviceInfo())
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationSeconds))
                .revoked(false)
                .build());

        return new AuthResponse(accessToken, refreshToken, user.getId(), user.getEmail());
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        Long userId = jwtTokenProvider.getUserId(refreshToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(TokenHashUtil.sha256(refreshToken))
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        if (stored.isExpired()) {
            throw new IllegalArgumentException("Refresh token expired");
        }
        if (!stored.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Refresh token user mismatch");
        }

        stored.setRevoked(true);

        User user = stored.getUser();
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHashUtil.sha256(newRefreshToken))
                .deviceInfo(stored.getDeviceInfo())
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationSeconds))
                .revoked(false)
                .build());

        return new AuthResponse(newAccessToken, newRefreshToken, user.getId(), user.getEmail());
    }

    @Transactional
    public void logout(String refreshToken) {
        String hash = TokenHashUtil.sha256(refreshToken);
        refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                .ifPresent(token -> token.setRevoked(true));
    }

    private User findOrCreateUserByGooglePayload(GoogleIdTokenPayload payload) {
        return userSocialAccountRepository.findByProviderAndProviderUserId(SocialProvider.GOOGLE, payload.sub())
                .map(UserSocialAccount::getUser)
                .orElseGet(() -> linkOrCreateUser(payload));
    }

    private User linkOrCreateUser(GoogleIdTokenPayload payload) {
        User user = userRepository.findByEmail(payload.email())
                .orElseGet(() -> userRepository.save(User.of(generateNickName(payload.email()), payload.email(), null)));

        userSocialAccountRepository.findByProviderAndProviderUserId(SocialProvider.GOOGLE, payload.sub())
                .orElseGet(() -> userSocialAccountRepository.save(UserSocialAccount.builder()
                        .user(user)
                        .provider(SocialProvider.GOOGLE)
                        .providerUserId(payload.sub())
                        .email(payload.email())
                        .build()));

        return user;
    }

    private String generateNickName(String email) {
        String localPart = email.split("@")[0];
        return "g_" + localPart;
    }
}
