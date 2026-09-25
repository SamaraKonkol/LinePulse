package com.linepulse.downtime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateDowntimeRequest(
        @NotNull UUID machineId,
        UUID incidentId,
        @NotBlank String reason,
        Instant startedAt
) {
}
