package com.linepulse.dashboard;

public record DashboardMetrics(
        long totalMachines,
        long activeMachines,
        long openIncidents,
        long activeWorkOrders,
        double availabilityPercentage,
        double mttrMinutes
) {
}
