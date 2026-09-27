package com.linepulse.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.linepulse.asset.Machine;
import com.linepulse.asset.MachineRepository;
import com.linepulse.asset.MachineStatus;
import com.linepulse.downtime.DowntimeRepository;
import com.linepulse.incident.IncidentRepository;
import com.linepulse.incident.IncidentStatus;
import com.linepulse.maintenance.MaintenanceType;
import com.linepulse.maintenance.WorkOrder;
import com.linepulse.maintenance.WorkOrderPriority;
import com.linepulse.maintenance.WorkOrderRepository;
import com.linepulse.maintenance.WorkOrderStatus;
import com.linepulse.organization.Organization;
import com.linepulse.organization.OrganizationService;
import com.linepulse.organization.OrganizationType;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {
    @Test
    void shouldCalculateMttrFromCompletedOrdersInLastThirtyDays() {
        MachineRepository machineRepository = mock(MachineRepository.class);
        IncidentRepository incidentRepository = mock(IncidentRepository.class);
        WorkOrderRepository workOrderRepository = mock(WorkOrderRepository.class);
        DowntimeRepository downtimeRepository = mock(DowntimeRepository.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        UUID organizationId = UUID.randomUUID();
        when(organizationService.currentOrganization()).thenReturn(new Organization(organizationId, "Empresa Teste", "empresa-teste", OrganizationType.COMPANY, true, Instant.now()));
        DashboardService service = new DashboardService(machineRepository, incidentRepository, workOrderRepository, downtimeRepository, organizationService);

        Instant startedAt = Instant.now().minus(Duration.ofMinutes(45));
        Machine machine = mock(Machine.class);
        WorkOrder order = new WorkOrder(UUID.randomUUID(), machine, null, "Reparo", "Reparo corretivo", MaintenanceType.CORRECTIVE, WorkOrderPriority.HIGH, WorkOrderStatus.OPEN, null, startedAt, startedAt);
        order.start(startedAt);
        order.complete(startedAt.plus(Duration.ofMinutes(45)));

        when(machineRepository.countByProductionLine_Sector_Plant_Organization_IdAndStatusNot(organizationId, MachineStatus.INACTIVE)).thenReturn(1L);
        when(machineRepository.countByProductionLine_Sector_Plant_Organization_IdAndStatus(organizationId, MachineStatus.RUNNING)).thenReturn(1L);
        when(incidentRepository.countByMachine_ProductionLine_Sector_Plant_Organization_IdAndStatusIn(organizationId, List.of(IncidentStatus.OPEN, IncidentStatus.IN_PROGRESS))).thenReturn(0L);
        when(workOrderRepository.countByMachine_ProductionLine_Sector_Plant_Organization_IdAndStatusIn(organizationId, List.of(WorkOrderStatus.OPEN, WorkOrderStatus.IN_PROGRESS))).thenReturn(0L);
        when(workOrderRepository.findByMachine_ProductionLine_Sector_Plant_Organization_IdAndStatusAndCompletedAtAfter(eq(organizationId), eq(WorkOrderStatus.COMPLETED), any(Instant.class))).thenReturn(List.of(order));
        when(downtimeRepository.findOverlappingByOrganization(eq(organizationId), any(Instant.class), any(Instant.class))).thenReturn(List.of());

        DashboardMetrics metrics = service.getMetrics();

        assertEquals(45.0, metrics.mttrMinutes());
        assertEquals(100.0, metrics.availabilityPercentage());
    }
}
