package com.linepulse.auth;

import java.util.Set;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String name,
        String registration,
        UserRole role,
        boolean active,
        boolean demoAccount
) {
    private static final Set<String> DEMO_REGISTRATIONS = Set.of("ADM001", "TEC001", "OPE001");

    static AdminUserResponse from(UserAccount user) {
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getRegistration(),
                user.getRole(),
                user.isActive(),
                DEMO_REGISTRATIONS.contains(user.getRegistration())
        );
    }
}
