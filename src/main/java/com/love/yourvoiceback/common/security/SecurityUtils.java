package com.love.yourvoiceback.common.security;

import com.love.yourvoiceback.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthenticatedUser getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new IllegalStateException("No authenticated user found in security context.");
        }
        return authenticatedUser;
    }

    public static User getCurrentUser() {
        return getCurrentAuthenticatedUser().getUser();
    }

    public static Long getCurrentUserId() {
        return getCurrentAuthenticatedUser().getUserId();
    }
}
