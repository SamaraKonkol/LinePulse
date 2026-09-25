package com.linepulse.incident;

import java.time.Instant;
import java.util.UUID;

public record IncidentResponse(
        UUID id,
        UUID machineId,
        String assetCode,
        String machineName,
        String title,
        String description,
        IncidentPriority priority,
        IncidentStatus status,
        Instant occurredAt,
        Instant createdAt
) {
    static IncidentResponse from(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getMachine().getId(),
                incident.getMachine().getAssetCode(),
                incident.getMachine().getName(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getPriority(),
                incident.getStatus(),
                incident.getOccurredAt(),
                incident.getCreatedAt()
        );
    }
}
