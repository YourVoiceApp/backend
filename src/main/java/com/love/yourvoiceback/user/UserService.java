package com.love.yourvoiceback.user;

import com.love.yourvoiceback.auth.repository.RefreshTokenRepository;
import com.love.yourvoiceback.auth.repository.UserSocialAccountRepository;
import com.love.yourvoiceback.common.exception.ApiException;
import com.love.yourvoiceback.common.exception.ErrorCode;
import com.love.yourvoiceback.user.dto.ChangePasswordRequest;
import com.love.yourvoiceback.user.dto.MeResponse;
import com.love.yourvoiceback.user.dto.SocialAccountResponse;
import com.love.yourvoiceback.user.dto.UpdateProfileRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserSocialAccountRepository userSocialAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MeResponse getMe(Long userId) {
        return MeResponse.from(getUser(userId));
    }

    @Transactional
    public MeResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUser(userId);

        if (userRepository.existsByNickNameAndIdNot(request.nickName(), userId)) {
            throw ApiException.error(ErrorCode.NICKNAME_ALREADY_TAKEN);
        }

        user.setNickName(request.nickName());
        return MeResponse.from(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getUser(userId);

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.newPassword()));
            return;
        }

        if (request.currentPassword() == null || request.currentPassword().isBlank()) {
            throw ApiException.error(ErrorCode.CURRENT_PASSWORD_REQUIRED);
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw ApiException.error(ErrorCode.CURRENT_PASSWORD_INCORRECT);
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public List<SocialAccountResponse> getSocialAccounts(Long userId) {
        return userSocialAccountRepository.findAllByUserId(userId).stream()
                .map(SocialAccountResponse::from)
                .toList();
    }

    @Transactional
    public void deleteMe(Long userId) {
        getUser(userId);
        refreshTokenRepository.deleteAllByUserId(userId);
        userSocialAccountRepository.deleteAllByUserId(userId);
        userRepository.deleteById(userId);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.error(ErrorCode.USER_NOT_FOUND));
    }
}
