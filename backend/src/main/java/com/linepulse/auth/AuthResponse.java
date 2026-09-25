package com.linepulse.auth;

public record AuthResponse(
        String token,
        AuthUserResponse user
) {
}
