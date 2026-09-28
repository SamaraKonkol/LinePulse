package com.linepulse.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.linepulse.asset.Machine;
import com.linepulse.downtime.DowntimeResponse;
import com.linepulse.downtime.DowntimeService;
import com.linepulse.incident.IncidentCategory;
import com.linepulse.incident.IncidentPriority;
import com.linepulse.incident.IncidentResponse;
import com.linepulse.incident.IncidentService;
import com.linepulse.incident.IncidentStatus;
import com.linepulse.maintenance.MaintenancePlanResponse;
import com.linepulse.maintenance.MaintenancePlanService;
import com.linepulse.maintenance.MaintenanceType;
import com.linepulse.maintenance.WorkOrderPriority;
import com.linepulse.maintenance.WorkOrderResponse;
import com.linepulse.maintenance.WorkOrderService;
import com.linepulse.maintenance.WorkOrderStatus;
import com.linepulse.organization.Organization;
import com.linepulse.organization.OrganizationAccessService;
import com.linepulse.organization.OrganizationMembership;
import com.linepulse.organization.OrganizationType;
import com.linepulse.service.ServiceRequest;
import com.linepulse.service.ServiceRequestPriority;
import com.linepulse.service.ServiceRequestRepository;
import com.linepulse.service.ServiceRequestStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlertServiceTest {
    @Test
    void shouldCreateAlertsOnlyForActiveOperationalRisks() {
        IncidentService incidentService = mock(IncidentService.class);
        WorkOrderService workOrderService = mock(WorkOrderService.class);
        DowntimeService downtimeService = mock(DowntimeService.class);
        MaintenancePlanService maintenancePlanService = mock(MaintenancePlanService.class);
        OrganizationAccessService accessService = mock(OrganizationAccessService.class);
        ServiceRequestRepository serviceRequestRepository = mock(ServiceRequestRepository.class);
        Organization company = organization(OrganizationType.COMPANY, "Empresa Teste");
        OrganizationMembership membership = membership(company);
        AlertService service = new AlertService(incidentService, workOrderService, downtimeService, maintenancePlanService, accessService, serviceRequestRepository);
        Instant now = Instant.now();

        IncidentResponse criticalIncident = new IncidentResponse(
                UUID.randomUUID(), UUID.randomUUID(), "PR-04", "Prensa 04", "Pressão instável", "Oscilação severa",
                IncidentCategory.HYDRAULIC, IncidentPriority.CRITICAL, IncidentStatus.OPEN, null, null,
                now.minus(Duration.ofMinutes(20)), null, now.minus(Duration.ofMinutes(20))
        );
        IncidentResponse resolvedIncident = new IncidentResponse(
                UUID.randomUUID(), UUID.randomUUID(), "ES-02", "Esteira 02", "Sensor", "Resolvido",
                IncidentCategory.ELECTRICAL, IncidentPriority.CRITICAL, IncidentStatus.RESOLVED, "Sensor danificado", "Sensor substituído",
                now.minus(Duration.ofHours(2)), now.minus(Duration.ofHours(1)), now.minus(Duration.ofHours(2))
        );
        DowntimeResponse longDowntime = new DowntimeResponse(
                UUID.randomUUID(), UUID.randomUUID(), "TR-01", "Torno 01", null, "Falha elétrica",
                now.minus(Duration.ofHours(2)), null, now.minus(Duration.ofHours(2))
        );
        WorkOrderResponse criticalOrder = new WorkOrderResponse(
                UUID.randomUUID(), UUID.randomUUID(), "FR-03", "Fresadora 03", null, "Reparo urgente", "Trocar componente",
                MaintenanceType.CORRECTIVE, WorkOrderPriority.CRITICAL, WorkOrderStatus.OPEN, null, null, null, now.minus(Duration.ofMinutes(40))
        );
        MaintenancePlanResponse preventivePlan = new MaintenancePlanResponse(
                UUID.randomUUID(), UUID.randomUUID(), "PR-04", "Prensa 04", "Troca de óleo", "Substituir óleo do redutor",
                90, LocalDate.now().plusDays(3), WorkOrderPriority.MEDIUM, true, null
        );

        when(accessService.currentMembership()).thenReturn(membership);
        when(incidentService.findAll()).thenReturn(List.of(criticalIncident, resolvedIncident));
        when(downtimeService.findAll()).thenReturn(List.of(longDowntime));
        when(workOrderService.findAll()).thenReturn(List.of(criticalOrder));
        when(maintenancePlanService.findDueThrough(LocalDate.now().plusDays(7))).thenReturn(List.of(preventivePlan));
        when(serviceRequestRepository.findTop100ByCompany_IdAndStatusInOrderByRequestedAtDesc(eq(company.getId()), anyCollection())).thenReturn(List.of());

        List<OperationalAlert> alerts = service.findActive();

        assertEquals(4, alerts.size());
        assertEquals(AlertSeverity.CRITICAL, alerts.getFirst().severity());
        assertEquals(1, alerts.stream().filter(alert -> alert.sourceType().equals("INCIDENT")).count());
        assertEquals(1, alerts.stream().filter(alert -> alert.sourceType().equals("DOWNTIME")).count());
        assertEquals(1, alerts.stream().filter(alert -> alert.sourceType().equals("WORK_ORDER")).count());
        assertEquals(1, alerts.stream().filter(alert -> alert.sourceType().equals("MAINTENANCE_PLAN")).count());
    }

    @Test
    void shouldAlertProviderAboutNewCriticalRequestAndExpiredEta() {
        IncidentService incidentService = mock(IncidentService.class);
        WorkOrderService workOrderService = mock(WorkOrderService.class);
        DowntimeService downtimeService = mock(DowntimeService.class);
        MaintenancePlanService maintenancePlanService = mock(MaintenancePlanService.class);
        OrganizationAccessService accessService = mock(OrganizationAccessService.class);
        ServiceRequestRepository serviceRequestRepository = mock(ServiceRequestRepository.class);
        Organization provider = organization(OrganizationType.SERVICE_PROVIDER, "Manutenção Norte");
        Organization company = organization(OrganizationType.COMPANY, "Fábrica Sul");
        OrganizationMembership membership = membership(provider);
        AlertService service = new AlertService(incidentService, workOrderService, downtimeService, maintenancePlanService, accessService, serviceRequestRepository);
        Instant now = Instant.now();

        ServiceRequest incoming = serviceRequest(company, ServiceRequestStatus.REQUESTED, ServiceRequestPriority.CRITICAL, now.minusSeconds(300), null, null);
        ServiceRequest overdue = serviceRequest(company, ServiceRequestStatus.ACCEPTED, ServiceRequestPriority.HIGH, now.minusSeconds(7200), now.minusSeconds(600), null);

        stubEmptyOperationalData(incidentService, workOrderService, downtimeService, maintenancePlanService);
        when(accessService.currentMembership()).thenReturn(membership);
        when(serviceRequestRepository.findTop100ByProvider_IdAndStatusInOrderByRequestedAtDesc(eq(provider.getId()), anyCollection()))
                .thenReturn(List.of(incoming, overdue));

        List<OperationalAlert> alerts = service.findActive();

        assertEquals(2, alerts.size());
        assertEquals(2, alerts.stream().filter(alert -> alert.sourceType().equals("SERVICE_REQUEST")).count());
        assertEquals(2, alerts.stream().filter(alert -> alert.severity() == AlertSeverity.CRITICAL).count());
        assertEquals(1, alerts.stream().filter(alert -> alert.title().equals("Novo chamado externo crítico")).count());
        assertEquals(1, alerts.stream().filter(alert -> alert.title().equals("Previsão de atendimento vencida")).count());
    }

    @Test
    void shouldAlertCompanyWhenCompletedServiceNeedsApproval() {
        IncidentService incidentService = mock(IncidentService.class);
        WorkOrderService workOrderService = mock(WorkOrderService.class);
        DowntimeService downtimeService = mock(DowntimeService.class);
        MaintenancePlanService maintenancePlanService = mock(MaintenancePlanService.class);
        OrganizationAccessService accessService = mock(OrganizationAccessService.class);
        ServiceRequestRepository serviceRequestRepository = mock(ServiceRequestRepository.class);
        Organization company = organization(OrganizationType.COMPANY, "Fábrica Sul");
        OrganizationMembership membership = membership(company);
        AlertService service = new AlertService(incidentService, workOrderService, downtimeService, maintenancePlanService, accessService, serviceRequestRepository);
        Instant now = Instant.now();

        ServiceRequest completed = serviceRequest(company, ServiceRequestStatus.COMPLETED, ServiceRequestPriority.MEDIUM, now.minusSeconds(7200), null, now.minusSeconds(300));

        stubEmptyOperationalData(incidentService, workOrderService, downtimeService, maintenancePlanService);
        when(accessService.currentMembership()).thenReturn(membership);
        when(serviceRequestRepository.findTop100ByCompany_IdAndStatusInOrderByRequestedAtDesc(eq(company.getId()), anyCollection()))
                .thenReturn(List.of(completed));

        List<OperationalAlert> alerts = service.findActive();

        assertEquals(1, alerts.size());
        assertEquals("SERVICE_REQUEST", alerts.getFirst().sourceType());
        assertEquals("Atendimento aguardando aprovação", alerts.getFirst().title());
        assertEquals(AlertSeverity.WARNING, alerts.getFirst().severity());
    }

    private static void stubEmptyOperationalData(
            IncidentService incidentService,
            WorkOrderService workOrderService,
            DowntimeService downtimeService,
            MaintenancePlanService maintenancePlanService
    ) {
        when(incidentService.findAll()).thenReturn(List.of());
        when(workOrderService.findAll()).thenReturn(List.of());
        when(downtimeService.findAll()).thenReturn(List.of());
        when(maintenancePlanService.findDueThrough(LocalDate.now().plusDays(7))).thenReturn(List.of());
    }

    private static Organization organization(OrganizationType type, String name) {
        return new Organization(UUID.randomUUID(), name, name.toLowerCase().replace(' ', '-'), type, true, Instant.now());
    }

    private static OrganizationMembership membership(Organization organization) {
        OrganizationMembership membership = mock(OrganizationMembership.class);
        when(membership.getOrganization()).thenReturn(organization);
        return membership;
    }

    private static ServiceRequest serviceRequest(
            Organization company,
            ServiceRequestStatus status,
            ServiceRequestPriority priority,
            Instant requestedAt,
            Instant eta,
            Instant completedAt
    ) {
        ServiceRequest request = mock(ServiceRequest.class);
        Machine machine = mock(Machine.class);
        UUID id = UUID.randomUUID();
        when(machine.getAssetCode()).thenReturn("PR-04");
        when(machine.getName()).thenReturn("Prensa 04");
        when(request.getId()).thenReturn(id);
        when(request.getCompany()).thenReturn(company);
        when(request.getMachine()).thenReturn(machine);
        when(request.getTitle()).thenReturn("Falha hidráulica");
        when(request.getStatus()).thenReturn(status);
        when(request.getPriority()).thenReturn(priority);
        when(request.getRequestedAt()).thenReturn(requestedAt);
        when(request.getEta()).thenReturn(eta);
        when(request.getCompletedAt()).thenReturn(completedAt);
        return request;
    }
}
