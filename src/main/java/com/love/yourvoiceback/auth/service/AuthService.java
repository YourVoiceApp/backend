package com.love.yourvoiceback.auth.service;

import com.love.yourvoiceback.auth.domain.RefreshToken;
import com.love.yourvoiceback.auth.domain.SocialProvider;
import com.love.yourvoiceback.auth.domain.UserSocialAccount;
import com.love.yourvoiceback.auth.dto.AuthResponse;
import com.love.yourvoiceback.auth.dto.GoogleLoginRequest;
import com.love.yourvoiceback.auth.dto.KakaoLoginRequest;
import com.love.yourvoiceback.auth.dto.LoginRequest;
import com.love.yourvoiceback.auth.dto.SignupRequest;
import com.love.yourvoiceback.auth.google.GoogleIdTokenPayload;
import com.love.yourvoiceback.auth.google.GoogleTokenVerifier;
import com.love.yourvoiceback.common.jwt.JwtTokenProvider;
import com.love.yourvoiceback.auth.kakao.KakaoAuthClient;
import com.love.yourvoiceback.auth.kakao.KakaoUserResponse;
import com.love.yourvoiceback.auth.repository.RefreshTokenRepository;
import com.love.yourvoiceback.auth.repository.UserSocialAccountRepository;
import com.love.yourvoiceback.common.jwt.TokenHashUtil;
import com.love.yourvoiceback.common.exception.ApiException;
import com.love.yourvoiceback.common.exception.ErrorCode;
import com.love.yourvoiceback.user.User;
import com.love.yourvoiceback.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final KakaoAuthClient kakaoAuthClient;
    private final EmailVerificationService emailVerificationService;
    private final UserSocialAccountRepository userSocialAccountRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${auth.jwt.refresh-expiration-seconds}")
    private long refreshExpirationSeconds;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        emailVerificationService.ensureVerified(request.email());
        AuthResponse response = signupWithoutEmailVerification(request);
        emailVerificationService.clearVerification(request.email());
        return response;
    }

    @Transactional
    public AuthResponse signupWithoutEmailVerification(SignupRequest request) {
        validateEmailSignupRequest(request);
        User user = createEmailUser(request);
        return issueTokens(user, request.deviceInfo());
    }

    private void validateEmailSignupRequest(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw ApiException.error(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        if (userRepository.existsByNickName(request.nickName())) {
            throw ApiException.error(ErrorCode.NICKNAME_ALREADY_TAKEN);
        }
    }

    private User createEmailUser(SignupRequest request) {
        return userRepository.save(User.of(
                request.nickName(),
                request.email(),
                passwordEncoder.encode(request.password())
        ));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> ApiException.error(ErrorCode.INVALID_CREDENTIALS));

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw ApiException.error(ErrorCode.PASSWORD_LOGIN_NOT_AVAILABLE);
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw ApiException.error(ErrorCode.INVALID_CREDENTIALS);
        }

        return issueTokens(user, request.deviceInfo());
    }

    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleIdTokenPayload payload = googleTokenVerifier.verify(request.idToken());
        User user = findOrCreateUserByGooglePayload(payload);

        return issueTokens(user, request.deviceInfo());
    }

    @Transactional
    public AuthResponse loginWithKakao(KakaoLoginRequest request) {
        KakaoUserResponse payload = kakaoAuthClient.getUserByAccessToken(request.accessToken());
        User user = findOrCreateUserByKakaoPayload(payload);

        return issueTokens(user, request.deviceInfo());
    }

    private AuthResponse issueTokens(User user, String deviceInfo) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHashUtil.sha256(refreshToken))
                .deviceInfo(deviceInfo)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationSeconds))
                .revoked(false)
                .build());

        return AuthResponse.of(accessToken, refreshToken, user.getId(), user.getEmail());
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw ApiException.error(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        Long userId = jwtTokenProvider.getUserId(refreshToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(TokenHashUtil.sha256(refreshToken))
                .orElseThrow(() -> ApiException.error(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (stored.isExpired()) {
            throw ApiException.error(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }
        if (!stored.getUser().getId().equals(userId)) {
            throw ApiException.error(ErrorCode.REFRESH_TOKEN_USER_MISMATCH);
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

        return AuthResponse.of(newAccessToken, newRefreshToken, user.getId(), user.getEmail());
    }

    @Transactional
    public void logout(String refreshToken) {
        String hash = TokenHashUtil.sha256(refreshToken);
        refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                .ifPresent(token -> token.setRevoked(true));
    }

    private User findOrCreateUserByGooglePayload(GoogleIdTokenPayload payload) {
        return findOrCreateSocialUser(SocialProvider.GOOGLE, payload.sub(), payload.email(), "g_");
    }

    private User findOrCreateUserByKakaoPayload(KakaoUserResponse payload) {
        return findOrCreateSocialUser(SocialProvider.KAKAO, payload.providerUserId(), payload.email(), "k_");
    }

    private User findOrCreateSocialUser(SocialProvider provider, String providerUserId, String email, String nicknamePrefix) {
        return userSocialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .map(UserSocialAccount::getUser)
                .orElseGet(() -> linkOrCreateUser(provider, providerUserId, email, nicknamePrefix));
    }

    private User linkOrCreateUser(SocialProvider provider, String providerUserId, String email, String nicknamePrefix) {
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(User.of(generateUniqueNickName(email, nicknamePrefix), email, null)));

        userSocialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .orElseGet(() -> userSocialAccountRepository.save(UserSocialAccount.builder()
                        .user(user)
                        .provider(provider)
                        .providerUserId(providerUserId)
                        .email(email)
                        .build()));

        return user;
    }

    private String generateUniqueNickName(String email, String prefix) {
        String localPart = email.split("@")[0];
        String base = prefix + localPart;

        if (!userRepository.existsByNickName(base)) {
            return base;
        }

        int suffix = 1;
        while (userRepository.existsByNickName(base + suffix)) {
            suffix++;
        }
        return base + suffix;
    }
}
