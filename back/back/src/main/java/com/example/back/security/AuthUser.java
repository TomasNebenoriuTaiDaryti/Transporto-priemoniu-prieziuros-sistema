package com.example.back.security;

import org.springframework.security.core.Authentication;

public final class AuthUser {
    private AuthUser() {}

    public static Long userId(Authentication auth) {
        Object p = auth.getPrincipal();
        if (p instanceof JwtAuthFilter.UserPrincipal up) return up.userId();
        throw new IllegalStateException("No authenticated user");
    }
}
