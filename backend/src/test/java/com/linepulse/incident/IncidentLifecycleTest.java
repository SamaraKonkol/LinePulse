package com.linepulse.incident;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IncidentLifecycleTest {
    @Test
    void shouldAllowOnlyValidLifecycleTransitions() {
        assertTrue(IncidentLifecycle.canStart(IncidentStatus.OPEN));
        assertFalse(IncidentLifecycle.canStart(IncidentStatus.IN_PROGRESS));

        assertTrue(IncidentLifecycle.canResolve(IncidentStatus.IN_PROGRESS));
        assertFalse(IncidentLifecycle.canResolve(IncidentStatus.OPEN));

        assertTrue(IncidentLifecycle.canCancel(IncidentStatus.OPEN));
        assertTrue(IncidentLifecycle.canCancel(IncidentStatus.IN_PROGRESS));
        assertFalse(IncidentLifecycle.canCancel(IncidentStatus.RESOLVED));
        assertFalse(IncidentLifecycle.canCancel(IncidentStatus.CANCELLED));
    }
}
