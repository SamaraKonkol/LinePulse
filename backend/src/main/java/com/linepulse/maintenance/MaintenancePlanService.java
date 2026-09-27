package com.linepulse.maintenance;

import com.linepulse.asset.Machine;
import com.linepulse.asset.MachineRepository;
import com.linepulse.audit.AuditService;
import com.linepulse.common.NotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenancePlanService {
    private final MaintenancePlanRepository maintenancePlanRepository;
    private final MachineRepository machineRepository;
    private final WorkOrderService workOrderService;
    private final AuditService auditService;

    public MaintenancePlanService(MaintenancePlanRepository maintenancePlanRepository, MachineRepository machineRepository, WorkOrderService workOrderService, AuditService auditService) {
        this.maintenancePlanRepository = maintenancePlanRepository;
        this.machineRepository = machineRepository;
        this.workOrderService = workOrderService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<MaintenancePlanResponse> findAll() {
        return maintenancePlanRepository.findAllByOrderByNextDueDateAsc().stream()
                .map(MaintenancePlanResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MaintenancePlanResponse> findDueThrough(LocalDate date) {
        return maintenancePlanRepository.findByActiveTrueAndNextDueDateLessThanEqualOrderByNextDueDateAsc(date).stream()
                .map(MaintenancePlanResponse::from)
                .toList();
    }

    @Transactional
    public MaintenancePlanResponse create(MaintenancePlanRequest request) {
        Machine machine = findMachine(request.machineId());
        Instant now = Instant.now();
        MaintenancePlan saved = maintenancePlanRepository.save(new MaintenancePlan(
                UUID.randomUUID(),
                machine,
                request.title().trim(),
                request.description().trim(),
                request.intervalDays(),
                request.nextDueDate(),
                request.priority(),
                true,
                now,
                now
        ));
        auditService.record("MAINTENANCE_PLAN_CREATED", "MAINTENANCE_PLAN", saved.getId(), "Plano preventivo criado para " + machine.getAssetCode() + ": " + saved.getTitle());
        return MaintenancePlanResponse.from(saved);
    }

    @Transactional
    public MaintenancePlanResponse update(UUID id, MaintenancePlanRequest request) {
        MaintenancePlan plan = findPlan(id);
        Machine machine = findMachine(request.machineId());
        plan.update(machine, request.title().trim(), request.description().trim(), request.intervalDays(), request.nextDueDate(), request.priority(), Instant.now());
        auditService.record("MAINTENANCE_PLAN_UPDATED", "MAINTENANCE_PLAN", plan.getId(), "Plano preventivo atualizado para " + machine.getAssetCode() + ": " + plan.getTitle());
        return MaintenancePlanResponse.from(plan);
    }

    @Transactional
    public MaintenancePlanResponse changeStatus(UUID id, boolean active) {
        MaintenancePlan plan = findPlan(id);
        plan.changeActive(active, Instant.now());
        auditService.record("MAINTENANCE_PLAN_STATUS_CHANGED", "MAINTENANCE_PLAN", plan.getId(), "Plano preventivo " + (active ? "ativado" : "desativado") + " para " + plan.getMachine().getAssetCode());
        return MaintenancePlanResponse.from(plan);
    }

    @Transactional
    public MaintenancePlanResponse generate(UUID id) {
        return generatePlan(findPlan(id), LocalDate.now());
    }

    @Transactional
    public int generateDuePlans() {
        LocalDate today = LocalDate.now();
        List<MaintenancePlan> duePlans = maintenancePlanRepository.findByActiveTrueAndNextDueDateLessThanEqual(today);
        duePlans.forEach(plan -> generatePlan(plan, today));
        return duePlans.size();
    }

    private MaintenancePlanResponse generatePlan(MaintenancePlan plan, LocalDate referenceDate) {
        Instant scheduledFor = plan.getNextDueDate().atTime(12, 0).toInstant(ZoneOffset.UTC);
        workOrderService.createPreventive(plan.getMachine(), plan.getTitle(), plan.getDescription(), plan.getPriority(), scheduledFor);
        Instant now = Instant.now();
        plan.markGenerated(now, referenceDate);
        auditService.record("MAINTENANCE_PLAN_GENERATED", "MAINTENANCE_PLAN", plan.getId(), "Preventiva gerada para " + plan.getMachine().getAssetCode() + "; próxima em " + plan.getNextDueDate());
        return MaintenancePlanResponse.from(plan);
    }

    private MaintenancePlan findPlan(UUID id) {
        return maintenancePlanRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Maintenance plan not found"));
    }

    private Machine findMachine(UUID id) {
        return machineRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Machine not found"));
    }
}
