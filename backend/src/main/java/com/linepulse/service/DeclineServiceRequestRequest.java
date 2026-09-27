package com.linepulse.service;

import jakarta.validation.constraints.NotBlank;

public record DeclineServiceRequestRequest(@NotBlank String reason) {
}
