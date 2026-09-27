package com.linepulse.maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MaintenancePlanTest {
    @Test
    void shouldAdvanceNextDueDatePastReferenceDateWithoutCreatingBacklog() {
        LocalDate initialDueDate = LocalDate.of(2026, 1, 1);
        LocalDate referenceDate = LocalDate.of(2026, 9, 27);
        Instant now = Instant.parse("2026-09-27T12:00:00Z");
        MaintenancePlan plan = new MaintenancePlan(
                UUID.randomUUID(), null, "Troca de óleo", "Substituir óleo", 90,
                initialDueDate, WorkOrderPriority.MEDIUM, true, now.minusSeconds(3600), now.minusSeconds(3600)
        );

        plan.markGenerated(now, referenceDate);

        assertEquals(LocalDate.of(2026, 9, 28), plan.getNextDueDate());
        assertEquals(now, plan.getLastGeneratedAt());
    }
}
