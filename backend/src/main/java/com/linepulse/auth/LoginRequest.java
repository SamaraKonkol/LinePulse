package com.linepulse.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 40) String registration,
        @NotBlank String password,
        @Size(max = 6) String otp
) {
    public LoginRequest(String registration, String password) { this(registration, password, null); }
}
