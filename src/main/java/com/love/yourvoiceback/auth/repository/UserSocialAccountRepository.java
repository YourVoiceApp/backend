package com.love.yourvoiceback.auth.repository;

import com.love.yourvoiceback.auth.domain.SocialProvider;
import com.love.yourvoiceback.auth.domain.UserSocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Long> {
    Optional<UserSocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);

    List<UserSocialAccount> findAllByUserId(Long userId);

    void deleteAllByUserId(Long userId);
}
