package com.linepulse.maintenance;

import com.linepulse.asset.Machine;
import com.linepulse.asset.MachineRepository;
import com.linepulse.common.ConflictException;
import com.linepulse.common.NotFoundException;
import com.linepulse.incident.Incident;
import com.linepulse.incident.IncidentRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkOrderService {
    private final WorkOrderRepository workOrderRepository;
    private final MachineRepository machineRepository;
    private final IncidentRepository incidentRepository;

    public WorkOrderService(WorkOrderRepository workOrderRepository, MachineRepository machineRepository, IncidentRepository incidentRepository) {
        this.workOrderRepository = workOrderRepository;
        this.machineRepository = machineRepository;
        this.incidentRepository = incidentRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkOrderResponse> findAll() {
        return workOrderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(WorkOrderResponse::from)
                .toList();
    }

    @Transactional
    public WorkOrderResponse create(CreateWorkOrderRequest request) {
        Machine machine = machineRepository.findById(request.machineId())
                .orElseThrow(() -> new NotFoundException("Machine not found"));
        Incident incident = resolveIncident(request.incidentId(), machine);
        Instant now = Instant.now();
        WorkOrder workOrder = new WorkOrder(
                UUID.randomUUID(),
                machine,
                incident,
                request.title().trim(),
                request.description().trim(),
                request.type(),
                request.priority(),
                WorkOrderStatus.OPEN,
                request.scheduledFor(),
                now,
                now
        );
        return WorkOrderResponse.from(workOrderRepository.save(workOrder));
    }

    @Transactional
    public WorkOrderResponse start(UUID id) {
        WorkOrder workOrder = findById(id);
        if (workOrder.getStatus() != WorkOrderStatus.OPEN) {
            throw new ConflictException("Only open work orders can be started");
        }
        workOrder.start(Instant.now());
        return WorkOrderResponse.from(workOrder);
    }

    @Transactional
    public WorkOrderResponse complete(UUID id) {
        WorkOrder workOrder = findById(id);
        if (workOrder.getStatus() != WorkOrderStatus.IN_PROGRESS) {
            throw new ConflictException("Only work orders in progress can be completed");
        }
        workOrder.complete(Instant.now());
        return WorkOrderResponse.from(workOrder);
    }

    private WorkOrder findById(UUID id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Work order not found"));
    }

    private Incident resolveIncident(UUID incidentId, Machine machine) {
        if (incidentId == null) {
            return null;
        }
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new NotFoundException("Incident not found"));
        if (!incident.getMachine().getId().equals(machine.getId())) {
            throw new IllegalArgumentException("Incident does not belong to selected machine");
        }
        return incident;
    }
}
