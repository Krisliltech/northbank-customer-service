package com.northbank.customerservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static boolean isCallerAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    public static UUID currentActorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        return (auth != null && auth.getPrincipal() instanceof UUID id) ? id : null;
    }
}
