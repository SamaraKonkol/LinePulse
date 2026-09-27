package com.linepulse.maintenance;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record MaintenancePlanRequest(
        @NotNull UUID machineId,
        @NotBlank String title,
        @NotBlank String description,
        @Min(1) int intervalDays,
        @NotNull LocalDate nextDueDate,
        @NotNull WorkOrderPriority priority
) {
}
