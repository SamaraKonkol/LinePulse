package com.linepulse.incident;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateIncidentRequest(
        @NotNull UUID machineId,
        @NotBlank String title,
        @NotBlank String description,
        @NotNull IncidentPriority priority,
        Instant occurredAt
) {
}
