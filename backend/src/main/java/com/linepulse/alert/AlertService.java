package com.linepulse.alert;

import com.linepulse.downtime.DowntimeResponse;
import com.linepulse.downtime.DowntimeService;
import com.linepulse.incident.IncidentPriority;
import com.linepulse.incident.IncidentResponse;
import com.linepulse.incident.IncidentService;
import com.linepulse.incident.IncidentStatus;
import com.linepulse.maintenance.WorkOrderPriority;
import com.linepulse.maintenance.WorkOrderResponse;
import com.linepulse.maintenance.WorkOrderService;
import com.linepulse.maintenance.WorkOrderStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {
    private final IncidentService incidentService;
    private final WorkOrderService workOrderService;
    private final DowntimeService downtimeService;

    public AlertService(IncidentService incidentService, WorkOrderService workOrderService, DowntimeService downtimeService) {
        this.incidentService = incidentService;
        this.workOrderService = workOrderService;
        this.downtimeService = downtimeService;
    }

    @Transactional(readOnly = true)
    public List<OperationalAlert> findActive() {
        Instant now = Instant.now();
        List<OperationalAlert> alerts = new ArrayList<>();

        incidentService.findAll().stream()
                .filter(this::isCriticalActiveIncident)
                .map(this::criticalIncidentAlert)
                .forEach(alerts::add);

        downtimeService.findAll().stream()
                .filter(downtime -> downtime.endedAt() == null)
                .filter(downtime -> Duration.between(downtime.startedAt(), now).toMinutes() >= 60)
                .map(downtime -> downtimeAlert(downtime, now))
                .forEach(alerts::add);

        workOrderService.findAll().stream()
                .filter(this::isCriticalActiveWorkOrder)
                .map(this::criticalWorkOrderAlert)
                .forEach(alerts::add);

        alerts.sort(Comparator
                .comparingInt((OperationalAlert alert) -> alert.severity() == AlertSeverity.CRITICAL ? 0 : 1)
                .thenComparing(OperationalAlert::detectedAt, Comparator.reverseOrder()));
        return alerts;
    }

    private boolean isCriticalActiveIncident(IncidentResponse incident) {
        return incident.priority() == IncidentPriority.CRITICAL
                && (incident.status() == IncidentStatus.OPEN || incident.status() == IncidentStatus.IN_PROGRESS);
    }

    private boolean isCriticalActiveWorkOrder(WorkOrderResponse order) {
        return order.priority() == WorkOrderPriority.CRITICAL
                && (order.status() == WorkOrderStatus.OPEN || order.status() == WorkOrderStatus.IN_PROGRESS);
    }

    private OperationalAlert criticalIncidentAlert(IncidentResponse incident) {
        return new OperationalAlert(
                "INCIDENT:" + incident.id(),
                AlertSeverity.CRITICAL,
                "Ocorrência crítica ativa",
                incident.assetCode() + " · " + incident.machineName() + " — " + incident.title(),
                "INCIDENT",
                incident.id(),
                incident.occurredAt()
        );
    }

    private OperationalAlert downtimeAlert(DowntimeResponse downtime, Instant now) {
        long minutes = Duration.between(downtime.startedAt(), now).toMinutes();
        AlertSeverity severity = minutes >= 240 ? AlertSeverity.CRITICAL : AlertSeverity.WARNING;
        return new OperationalAlert(
                "DOWNTIME:" + downtime.id(),
                severity,
                minutes >= 240 ? "Parada prolongada crítica" : "Parada prolongada",
                downtime.assetCode() + " · " + downtime.machineName() + " está parada há " + formatDuration(minutes),
                "DOWNTIME",
                downtime.id(),
                downtime.startedAt()
        );
    }

    private OperationalAlert criticalWorkOrderAlert(WorkOrderResponse order) {
        return new OperationalAlert(
                "WORK_ORDER:" + order.id(),
                AlertSeverity.WARNING,
                "Ordem crítica pendente",
                order.assetCode() + " · " + order.machineName() + " — " + order.title(),
                "WORK_ORDER",
                order.id(),
                order.createdAt()
        );
    }

    private String formatDuration(long minutes) {
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        if (hours == 0) {
            return remainingMinutes + " min";
        }
        if (remainingMinutes == 0) {
            return hours + "h";
        }
        return hours + "h " + remainingMinutes + "min";
    }
}
