package com.linepulse.dashboard;

import com.linepulse.asset.MachineRepository;
import com.linepulse.asset.MachineStatus;
import com.linepulse.downtime.Downtime;
import com.linepulse.downtime.DowntimeRepository;
import com.linepulse.incident.IncidentRepository;
import com.linepulse.incident.IncidentStatus;
import com.linepulse.maintenance.WorkOrderRepository;
import com.linepulse.maintenance.WorkOrderStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final MachineRepository machineRepository;
    private final IncidentRepository incidentRepository;
    private final WorkOrderRepository workOrderRepository;
    private final DowntimeRepository downtimeRepository;

    public DashboardService(MachineRepository machineRepository, IncidentRepository incidentRepository, WorkOrderRepository workOrderRepository, DowntimeRepository downtimeRepository) {
        this.machineRepository = machineRepository;
        this.incidentRepository = incidentRepository;
        this.workOrderRepository = workOrderRepository;
        this.downtimeRepository = downtimeRepository;
    }

    @Transactional(readOnly = true)
    public DashboardMetrics getMetrics() {
        long totalMachines = machineRepository.countByStatusNot(MachineStatus.INACTIVE);
        long activeMachines = machineRepository.countByStatus(MachineStatus.RUNNING);
        long openIncidents = incidentRepository.countByStatusIn(List.of(IncidentStatus.OPEN, IncidentStatus.IN_PROGRESS));
        long activeWorkOrders = workOrderRepository.countByStatusIn(List.of(WorkOrderStatus.OPEN, WorkOrderStatus.IN_PROGRESS));
        double availability = calculateAvailability(totalMachines);
        return new DashboardMetrics(totalMachines, activeMachines, openIncidents, activeWorkOrders, availability);
    }

    private double calculateAvailability(long machineCount) {
        if (machineCount == 0) {
            return 100.0;
        }
        Instant end = Instant.now();
        Instant start = end.minus(Duration.ofHours(24));
        long downtimeSeconds = downtimeRepository.findOverlapping(start, end).stream()
                .mapToLong(downtime -> overlapSeconds(downtime, start, end))
                .sum();
        double totalSeconds = machineCount * Duration.ofHours(24).toSeconds();
        double availability = Math.max(0.0, 100.0 * (1.0 - downtimeSeconds / totalSeconds));
        return Math.round(availability * 10.0) / 10.0;
    }

    private long overlapSeconds(Downtime downtime, Instant start, Instant end) {
        Instant effectiveStart = downtime.getStartedAt().isBefore(start) ? start : downtime.getStartedAt();
        Instant rawEnd = downtime.getEndedAt() == null ? end : downtime.getEndedAt();
        Instant effectiveEnd = rawEnd.isAfter(end) ? end : rawEnd;
        if (!effectiveEnd.isAfter(effectiveStart)) {
            return 0;
        }
        return Duration.between(effectiveStart, effectiveEnd).toSeconds();
    }
}
