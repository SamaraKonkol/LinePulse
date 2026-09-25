package com.linepulse.maintenance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateWorkOrderRequest(
        @NotNull UUID machineId,
        UUID incidentId,
        @NotBlank String title,
        @NotBlank String description,
        @NotNull MaintenanceType type,
        @NotNull WorkOrderPriority priority,
        Instant scheduledFor
) {
}
