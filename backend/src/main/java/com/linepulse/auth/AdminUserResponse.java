package com.linepulse.auth;

import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String name,
        String email,
        UserRole role,
        boolean active,
        boolean demoAccount
) {
    static AdminUserResponse from(UserAccount user) {
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getEmail().endsWith("@linepulse.local")
        );
    }
}
