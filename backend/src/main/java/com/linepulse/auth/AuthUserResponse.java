package com.linepulse.auth;

import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String name,
        String registration,
        UserRole role
) {
    static AuthUserResponse from(UserAccount user) {
        return new AuthUserResponse(user.getId(), user.getName(), user.getRegistration(), user.getRole());
    }
}
