package com.linepulse.auth;

import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String name,
        String email,
        UserRole role
) {
    static AuthUserResponse from(UserAccount user) {
        return new AuthUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
