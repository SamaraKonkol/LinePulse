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
        IncidentCategory category,
        IncidentPriority priority,
        IncidentStatus status,
        String rootCause,
        String solution,
        Instant occurredAt,
        Instant resolvedAt,
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
                incident.getCategory(),
                incident.getPriority(),
                incident.getStatus(),
                incident.getRootCause(),
                incident.getSolution(),
                incident.getOccurredAt(),
                incident.getResolvedAt(),
                incident.getCreatedAt()
        );
    }
}
