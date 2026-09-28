package com.linepulse.alert;

import com.linepulse.downtime.DowntimeResponse;
import com.linepulse.downtime.DowntimeService;
import com.linepulse.incident.IncidentPriority;
import com.linepulse.incident.IncidentResponse;
import com.linepulse.incident.IncidentService;
import com.linepulse.incident.IncidentStatus;
import com.linepulse.maintenance.MaintenancePlanResponse;
import com.linepulse.maintenance.MaintenancePlanService;
import com.linepulse.maintenance.WorkOrderPriority;
import com.linepulse.maintenance.WorkOrderResponse;
import com.linepulse.maintenance.WorkOrderService;
import com.linepulse.maintenance.WorkOrderStatus;
import com.linepulse.organization.Organization;
import com.linepulse.organization.OrganizationAccessService;
import com.linepulse.organization.OrganizationType;
import com.linepulse.service.ServiceRequest;
import com.linepulse.service.ServiceRequestPriority;
import com.linepulse.service.ServiceRequestRepository;
import com.linepulse.service.ServiceRequestStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {
    private final IncidentService incidentService;
    private final WorkOrderService workOrderService;
    private final DowntimeService downtimeService;
    private final MaintenancePlanService maintenancePlanService;
    private final OrganizationAccessService organizationAccessService;
    private final ServiceRequestRepository serviceRequestRepository;

    public AlertService(
            IncidentService incidentService,
            WorkOrderService workOrderService,
            DowntimeService downtimeService,
            MaintenancePlanService maintenancePlanService,
            OrganizationAccessService organizationAccessService,
            ServiceRequestRepository serviceRequestRepository
    ) {
        this.incidentService = incidentService;
        this.workOrderService = workOrderService;
        this.downtimeService = downtimeService;
        this.maintenancePlanService = maintenancePlanService;
        this.organizationAccessService = organizationAccessService;
        this.serviceRequestRepository = serviceRequestRepository;
    }

    @Transactional(readOnly = true)
    public List<OperationalAlert> findActive() {
        Instant now = Instant.now();
        LocalDate today = LocalDate.now();
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

        maintenancePlanService.findDueThrough(today.plusDays(7)).stream()
                .map(plan -> preventiveAlert(plan, today))
                .forEach(alerts::add);

        addServiceNetworkAlerts(alerts, now);

        alerts.sort(Comparator
                .comparingInt((OperationalAlert alert) -> alert.severity() == AlertSeverity.CRITICAL ? 0 : 1)
                .thenComparing(OperationalAlert::detectedAt, Comparator.reverseOrder()));
        return alerts;
    }

    private void addServiceNetworkAlerts(List<OperationalAlert> alerts, Instant now) {
        Organization organization = organizationAccessService.currentMembership().getOrganization();
        if (organization.getType() == OrganizationType.SERVICE_PROVIDER) {
            serviceRequestRepository.findTop100ByProvider_IdAndStatusInOrderByRequestedAtDesc(
                            organization.getId(),
                            Set.of(ServiceRequestStatus.REQUESTED, ServiceRequestStatus.ACCEPTED, ServiceRequestStatus.EN_ROUTE)
                    ).forEach(request -> {
                        if (request.getStatus() == ServiceRequestStatus.REQUESTED) {
                            alerts.add(incomingServiceRequestAlert(request));
                        } else if (request.getEta() != null && request.getEta().isBefore(now)) {
                            alerts.add(overdueEtaAlert(request));
                        }
                    });
            return;
        }

        serviceRequestRepository.findTop100ByCompany_IdAndStatusInOrderByRequestedAtDesc(
                        organization.getId(),
                        Set.of(ServiceRequestStatus.COMPLETED)
                ).stream()
                .map(this::pendingServiceApprovalAlert)
                .forEach(alerts::add);
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

    private OperationalAlert preventiveAlert(MaintenancePlanResponse plan, LocalDate today) {
        boolean overdue = plan.nextDueDate().isBefore(today);
        boolean dueToday = plan.nextDueDate().isEqual(today);
        String timing = overdue ? "vencida em " + plan.nextDueDate() : dueToday ? "vence hoje" : "vence em " + plan.nextDueDate();
        return new OperationalAlert(
                "MAINTENANCE_PLAN:" + plan.id(),
                overdue ? AlertSeverity.CRITICAL : AlertSeverity.WARNING,
                overdue ? "Preventiva vencida" : "Preventiva próxima",
                plan.assetCode() + " · " + plan.machineName() + " — " + plan.title() + " · " + timing,
                "MAINTENANCE_PLAN",
                plan.id(),
                plan.nextDueDate().atStartOfDay().toInstant(ZoneOffset.UTC)
        );
    }

    private OperationalAlert incomingServiceRequestAlert(ServiceRequest request) {
        boolean critical = request.getPriority() == ServiceRequestPriority.CRITICAL;
        return new OperationalAlert(
                "SERVICE_REQUEST:REQUESTED:" + request.getId(),
                critical ? AlertSeverity.CRITICAL : AlertSeverity.WARNING,
                critical ? "Novo chamado externo crítico" : "Novo chamado externo",
                request.getCompany().getName() + " · " + request.getMachine().getAssetCode() + " — " + request.getTitle(),
                "SERVICE_REQUEST",
                request.getId(),
                request.getRequestedAt()
        );
    }

    private OperationalAlert overdueEtaAlert(ServiceRequest request) {
        return new OperationalAlert(
                "SERVICE_REQUEST:ETA:" + request.getId(),
                AlertSeverity.CRITICAL,
                "Previsão de atendimento vencida",
                request.getCompany().getName() + " · " + request.getMachine().getAssetCode() + " — " + request.getTitle(),
                "SERVICE_REQUEST",
                request.getId(),
                request.getEta()
        );
    }

    private OperationalAlert pendingServiceApprovalAlert(ServiceRequest request) {
        return new OperationalAlert(
                "SERVICE_REQUEST:APPROVAL:" + request.getId(),
                AlertSeverity.WARNING,
                "Atendimento aguardando aprovação",
                request.getMachine().getAssetCode() + " · " + request.getMachine().getName() + " — " + request.getTitle(),
                "SERVICE_REQUEST",
                request.getId(),
                request.getCompletedAt() == null ? request.getRequestedAt() : request.getCompletedAt()
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
