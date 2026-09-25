package com.linepulse.incident;

public final class IncidentLifecycle {
    private IncidentLifecycle() {
    }

    public static boolean canStart(IncidentStatus status) {
        return status == IncidentStatus.OPEN;
    }

    public static boolean canResolve(IncidentStatus status) {
        return status == IncidentStatus.IN_PROGRESS;
    }

    public static boolean canCancel(IncidentStatus status) {
        return status == IncidentStatus.OPEN || status == IncidentStatus.IN_PROGRESS;
    }
}
