package com.linepulse.maintenance;

import com.linepulse.asset.Machine;
import com.linepulse.asset.MachineRepository;
import com.linepulse.audit.AuditService;
import com.linepulse.common.ConflictException;
import com.linepulse.common.NotFoundException;
import com.linepulse.common.PageResponse;
import com.linepulse.incident.Incident;
import com.linepulse.incident.IncidentRepository;
import com.linepulse.organization.OrganizationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkOrderService {
    private final WorkOrderRepository workOrderRepository;
    private final MachineRepository machineRepository;
    private final IncidentRepository incidentRepository;
    private final AuditService auditService;
    private final OrganizationService organizationService;

    public WorkOrderService(WorkOrderRepository workOrderRepository, MachineRepository machineRepository, IncidentRepository incidentRepository, AuditService auditService, OrganizationService organizationService) {
        this.workOrderRepository = workOrderRepository;
        this.machineRepository = machineRepository;
        this.incidentRepository = incidentRepository;
        this.auditService = auditService;
        this.organizationService = organizationService;
    }

    @Transactional(readOnly = true)
    public List<WorkOrderResponse> findAll() {
        UUID organizationId = organizationService.currentOrganization().getId();
        return workOrderRepository.findAllByMachine_ProductionLine_Sector_Plant_Organization_IdOrderByCreatedAtDesc(organizationId).stream()
                .map(WorkOrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkOrderResponse> findPage(int page, int size) {
        UUID organizationId = organizationService.currentOrganization().getId();
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<WorkOrder> result = workOrderRepository.findAllByMachine_ProductionLine_Sector_Plant_Organization_Id(
                organizationId,
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResponse.from(result, result.getContent().stream().map(WorkOrderResponse::from).toList());
    }

    @Transactional
    public WorkOrderResponse create(CreateWorkOrderRequest request) {
        UUID organizationId = organizationService.currentOrganization().getId();
        Machine machine = machineRepository.findByIdAndProductionLine_Sector_Plant_Organization_Id(request.machineId(), organizationId)
                .orElseThrow(() -> new NotFoundException("Machine not found"));
        Incident incident = resolveIncident(request.incidentId(), machine, organizationId);
        return createOrder(machine, incident, request.title(), request.description(), request.type(), request.priority(), request.scheduledFor(), "WORK_ORDER_CREATED");
    }

    @Transactional
    public WorkOrderResponse createPreventive(Machine machine, String title, String description, WorkOrderPriority priority, Instant scheduledFor) {
        return createOrder(machine, null, title, description, MaintenanceType.PREVENTIVE, priority, scheduledFor, "PREVENTIVE_WORK_ORDER_CREATED");
    }

    private WorkOrderResponse createOrder(Machine machine, Incident incident, String title, String description, MaintenanceType type, WorkOrderPriority priority, Instant scheduledFor, String auditAction) {
        Instant now = Instant.now();
        WorkOrder workOrder = new WorkOrder(
                UUID.randomUUID(), machine, incident, title.trim(), description.trim(), type, priority,
                WorkOrderStatus.OPEN, scheduledFor, now, now
        );
        WorkOrder saved = workOrderRepository.save(workOrder);
        auditService.record(auditAction, "WORK_ORDER", saved.getId(), "Ordem criada para " + machine.getAssetCode() + ": " + saved.getTitle());
        return WorkOrderResponse.from(saved);
    }

    @Transactional
    public WorkOrderResponse start(UUID id) {
        WorkOrder workOrder = findById(id);
        if (workOrder.getStatus() != WorkOrderStatus.OPEN) {
            throw new ConflictException("Only open work orders can be started");
        }
        workOrder.start(Instant.now());
        auditService.record("WORK_ORDER_STARTED", "WORK_ORDER", workOrder.getId(), "Ordem iniciada em " + workOrder.getMachine().getAssetCode());
        return WorkOrderResponse.from(workOrder);
    }

    @Transactional
    public WorkOrderResponse complete(UUID id) {
        WorkOrder workOrder = findById(id);
        if (workOrder.getStatus() != WorkOrderStatus.IN_PROGRESS) {
            throw new ConflictException("Only work orders in progress can be completed");
        }
        workOrder.complete(Instant.now());
        auditService.record("WORK_ORDER_COMPLETED", "WORK_ORDER", workOrder.getId(), "Ordem concluída em " + workOrder.getMachine().getAssetCode());
        return WorkOrderResponse.from(workOrder);
    }

    private WorkOrder findById(UUID id) {
        UUID organizationId = organizationService.currentOrganization().getId();
        return workOrderRepository.findByIdAndMachine_ProductionLine_Sector_Plant_Organization_Id(id, organizationId)
                .orElseThrow(() -> new NotFoundException("Work order not found"));
    }

    private Incident resolveIncident(UUID incidentId, Machine machine, UUID organizationId) {
        if (incidentId == null) return null;
        Incident incident = incidentRepository.findByIdAndMachine_ProductionLine_Sector_Plant_Organization_Id(incidentId, organizationId)
                .orElseThrow(() -> new NotFoundException("Incident not found"));
        if (!incident.getMachine().getId().equals(machine.getId())) {
            throw new IllegalArgumentException("Incident does not belong to selected machine");
        }
        return incident;
    }
}
