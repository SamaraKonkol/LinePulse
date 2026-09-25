package com.linepulse.maintenance;

import java.time.Instant;
import java.util.UUID;

public record WorkOrderResponse(
        UUID id,
        UUID machineId,
        String assetCode,
        String machineName,
        UUID incidentId,
        String title,
        String description,
        MaintenanceType type,
        WorkOrderPriority priority,
        WorkOrderStatus status,
        Instant scheduledFor,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt
) {
    static WorkOrderResponse from(WorkOrder workOrder) {
        return new WorkOrderResponse(
                workOrder.getId(),
                workOrder.getMachine().getId(),
                workOrder.getMachine().getAssetCode(),
                workOrder.getMachine().getName(),
                workOrder.getIncident() == null ? null : workOrder.getIncident().getId(),
                workOrder.getTitle(),
                workOrder.getDescription(),
                workOrder.getType(),
                workOrder.getPriority(),
                workOrder.getStatus(),
                workOrder.getScheduledFor(),
                workOrder.getStartedAt(),
                workOrder.getCompletedAt(),
                workOrder.getCreatedAt()
        );
    }
}
