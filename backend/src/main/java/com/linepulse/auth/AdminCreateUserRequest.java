package com.linepulse.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminCreateUserRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 40) String registration,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull UserRole role
) {
}
