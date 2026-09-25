package com.linepulse.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.linepulse.downtime.DowntimeResponse;
import com.linepulse.downtime.DowntimeService;
import com.linepulse.incident.IncidentPriority;
import com.linepulse.incident.IncidentResponse;
import com.linepulse.incident.IncidentService;
import com.linepulse.incident.IncidentStatus;
import com.linepulse.maintenance.MaintenanceType;
import com.linepulse.maintenance.WorkOrderPriority;
import com.linepulse.maintenance.WorkOrderResponse;
import com.linepulse.maintenance.WorkOrderService;
import com.linepulse.maintenance.WorkOrderStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlertServiceTest {
    @Test
    void shouldCreateAlertsOnlyForActiveOperationalRisks() {
        IncidentService incidentService = mock(IncidentService.class);
        WorkOrderService workOrderService = mock(WorkOrderService.class);
        DowntimeService downtimeService = mock(DowntimeService.class);
        AlertService service = new AlertService(incidentService, workOrderService, downtimeService);
        Instant now = Instant.now();

        IncidentResponse criticalIncident = new IncidentResponse(
                UUID.randomUUID(), UUID.randomUUID(), "PR-04", "Prensa 04", "Pressão instável", "Oscilação severa",
                IncidentPriority.CRITICAL, IncidentStatus.OPEN, now.minus(Duration.ofMinutes(20)), now.minus(Duration.ofMinutes(20))
        );
        IncidentResponse resolvedIncident = new IncidentResponse(
                UUID.randomUUID(), UUID.randomUUID(), "ES-02", "Esteira 02", "Sensor", "Resolvido",
                IncidentPriority.CRITICAL, IncidentStatus.RESOLVED, now.minus(Duration.ofHours(2)), now.minus(Duration.ofHours(2))
        );
        DowntimeResponse longDowntime = new DowntimeResponse(
                UUID.randomUUID(), UUID.randomUUID(), "TR-01", "Torno 01", null, "Falha elétrica",
                now.minus(Duration.ofHours(2)), null, now.minus(Duration.ofHours(2))
        );
        WorkOrderResponse criticalOrder = new WorkOrderResponse(
                UUID.randomUUID(), UUID.randomUUID(), "FR-03", "Fresadora 03", null, "Reparo urgente", "Trocar componente",
                MaintenanceType.CORRECTIVE, WorkOrderPriority.CRITICAL, WorkOrderStatus.OPEN, null, null, null, now.minus(Duration.ofMinutes(40))
        );

        when(incidentService.findAll()).thenReturn(List.of(criticalIncident, resolvedIncident));
        when(downtimeService.findAll()).thenReturn(List.of(longDowntime));
        when(workOrderService.findAll()).thenReturn(List.of(criticalOrder));

        List<OperationalAlert> alerts = service.findActive();

        assertEquals(3, alerts.size());
        assertEquals(AlertSeverity.CRITICAL, alerts.getFirst().severity());
        assertEquals(1, alerts.stream().filter(alert -> alert.sourceType().equals("INCIDENT")).count());
        assertEquals(1, alerts.stream().filter(alert -> alert.sourceType().equals("DOWNTIME")).count());
        assertEquals(1, alerts.stream().filter(alert -> alert.sourceType().equals("WORK_ORDER")).count());
    }
}
