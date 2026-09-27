package com.linepulse.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        AlertService service = new AlertService(incidentService, workOrderService, downtimeService, maintenancePlanService);
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

        when(incidentService.findAll()).thenReturn(List.of(criticalIncident, resolvedIncident));
        when(downtimeService.findAll()).thenReturn(List.of(longDowntime));
        when(workOrderService.findAll()).thenReturn(List.of(criticalOrder));
        when(maintenancePlanService.findDueThrough(LocalDate.now().plusDays(7))).thenReturn(List.of(preventivePlan));

        List<OperationalAlert> alerts = service.findActive();

        assertEquals(4, alerts.size());
        assertEquals(AlertSeverity.CRITICAL, alerts.getFirst().severity());
        assertEquals(1, alerts.stream().filter(alert -> alert.sourceType().equals("INCIDENT")).count());
        assertEquals(1, alerts.stream().filter(alert -> alert.sourceType().equals("DOWNTIME")).count());
        assertEquals(1, alerts.stream().filter(alert -> alert.sourceType().equals("WORK_ORDER")).count());
        assertEquals(1, alerts.stream().filter(alert -> alert.sourceType().equals("MAINTENANCE_PLAN")).count());
    }
}
