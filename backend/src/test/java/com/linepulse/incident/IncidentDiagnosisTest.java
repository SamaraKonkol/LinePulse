package com.linepulse.incident;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IncidentDiagnosisTest {
    @Test
    void shouldPersistDiagnosisWhenIncidentIsResolved() {
        Instant createdAt = Instant.parse("2026-09-27T10:00:00Z");
        Instant resolvedAt = Instant.parse("2026-09-27T11:00:00Z");
        Incident incident = new Incident(
                UUID.randomUUID(), null, "Ruído no redutor", "Ruído metálico durante operação",
                IncidentCategory.MECHANICAL, IncidentPriority.HIGH, IncidentStatus.IN_PROGRESS,
                createdAt, createdAt, createdAt
        );

        incident.resolve("Rolamento desgastado", "Rolamento substituído e conjunto relubrificado", resolvedAt);

        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
        assertEquals("Rolamento desgastado", incident.getRootCause());
        assertEquals("Rolamento substituído e conjunto relubrificado", incident.getSolution());
        assertEquals(resolvedAt, incident.getResolvedAt());
    }
}
