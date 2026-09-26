package com.linepulse.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 40) String registration,
        @NotBlank String password
) {
}
