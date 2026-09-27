package com.linepulse.maintenance;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MaintenancePlanResponse(
        UUID id,
        UUID machineId,
        String assetCode,
        String machineName,
        String title,
        String description,
        int intervalDays,
        LocalDate nextDueDate,
        WorkOrderPriority priority,
        boolean active,
        Instant lastGeneratedAt
) {
    static MaintenancePlanResponse from(MaintenancePlan plan) {
        return new MaintenancePlanResponse(
                plan.getId(),
                plan.getMachine().getId(),
                plan.getMachine().getAssetCode(),
                plan.getMachine().getName(),
                plan.getTitle(),
                plan.getDescription(),
                plan.getIntervalDays(),
                plan.getNextDueDate(),
                plan.getPriority(),
                plan.isActive(),
                plan.getLastGeneratedAt()
        );
    }
}
