package com.linepulse.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        String action,
        String entityType,
        UUID entityId,
        String description,
        String actorRegistration,
        Instant createdAt
) {
    static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getAction(),
                event.getEntityType(),
                event.getEntityId(),
                event.getDescription(),
                event.getActorRegistration(),
                event.getCreatedAt()
        );
    }
}
