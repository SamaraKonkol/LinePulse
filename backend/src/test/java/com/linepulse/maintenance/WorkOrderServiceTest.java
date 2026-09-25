package com.linepulse.maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.linepulse.asset.Machine;
import com.linepulse.asset.MachineRepository;
import com.linepulse.common.ConflictException;
import com.linepulse.incident.IncidentRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkOrderServiceTest {
    private WorkOrderRepository workOrderRepository;
    private WorkOrderService service;
    private Machine machine;

    @BeforeEach
    void setUp() {
        workOrderRepository = mock(WorkOrderRepository.class);
        service = new WorkOrderService(workOrderRepository, mock(MachineRepository.class), mock(IncidentRepository.class));
        machine = mock(Machine.class);
        when(machine.getId()).thenReturn(UUID.randomUUID());
        when(machine.getAssetCode()).thenReturn("PR-04");
        when(machine.getName()).thenReturn("Prensa hidráulica");
    }

    @Test
    void shouldStartAndCompleteOpenWorkOrder() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        WorkOrder workOrder = new WorkOrder(id, machine, null, "Falha hidráulica", "Inspecionar circuito", MaintenanceType.CORRECTIVE, WorkOrderPriority.HIGH, WorkOrderStatus.OPEN, null, now, now);
        when(workOrderRepository.findById(id)).thenReturn(Optional.of(workOrder));

        WorkOrderResponse started = service.start(id);
        WorkOrderResponse completed = service.complete(id);

        assertEquals(WorkOrderStatus.IN_PROGRESS, started.status());
        assertEquals(WorkOrderStatus.COMPLETED, completed.status());
    }

    @Test
    void shouldRejectStartingWorkOrderTwice() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        WorkOrder workOrder = new WorkOrder(id, machine, null, "Falha hidráulica", "Inspecionar circuito", MaintenanceType.CORRECTIVE, WorkOrderPriority.HIGH, WorkOrderStatus.OPEN, null, now, now);
        workOrder.start(now);
        when(workOrderRepository.findById(id)).thenReturn(Optional.of(workOrder));

        assertThrows(ConflictException.class, () -> service.start(id));
    }
}
