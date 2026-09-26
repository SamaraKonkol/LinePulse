package com.linepulse.asset;

import com.linepulse.audit.AuditService;
import com.linepulse.common.ConflictException;
import com.linepulse.common.NotFoundException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MachineService {
    private final MachineRepository machineRepository;
    private final ProductionLineRepository productionLineRepository;
    private final AuditService auditService;

    public MachineService(MachineRepository machineRepository, ProductionLineRepository productionLineRepository, AuditService auditService) {
        this.machineRepository = machineRepository;
        this.productionLineRepository = productionLineRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<MachineResponse> findAll() {
        return machineRepository.findAll().stream()
                .sorted(Comparator.comparing(Machine::getAssetCode, String.CASE_INSENSITIVE_ORDER))
                .map(MachineResponse::from)
                .toList();
    }

    @Transactional
    public MachineResponse create(CreateMachineRequest request) {
        String assetCode = request.assetCode().trim().toUpperCase();
        if (machineRepository.existsByAssetCodeIgnoreCase(assetCode)) {
            throw new ConflictException("Código de ativo já cadastrado.");
        }

        ProductionLine line = findAvailableProductionLine(request.productionLineId());
        Instant now = Instant.now();
        Machine machine = new Machine(
                UUID.randomUUID(),
                line,
                request.name().trim(),
                assetCode,
                normalize(request.manufacturer()),
                normalize(request.model()),
                normalize(request.serialNumber()),
                request.status(),
                request.installedAt(),
                now,
                now
        );
        Machine saved = machineRepository.save(machine);
        auditService.record("MACHINE_CREATED", "MACHINE", saved.getId(), "Máquina " + saved.getAssetCode() + " cadastrada");
        return MachineResponse.from(saved);
    }

    @Transactional
    public MachineResponse update(UUID machineId, UpdateMachineRequest request) {
        Machine machine = findMachine(machineId);
        String assetCode = request.assetCode().trim().toUpperCase();
        if (machineRepository.existsByAssetCodeIgnoreCaseAndIdNot(assetCode, machineId)) {
            throw new ConflictException("Código de ativo já cadastrado.");
        }

        ProductionLine line = findAvailableProductionLine(request.productionLineId());
        machine.updateDetails(
                line,
                request.name().trim(),
                assetCode,
                normalize(request.manufacturer()),
                normalize(request.model()),
                normalize(request.serialNumber()),
                request.installedAt()
        );
        auditService.record("MACHINE_UPDATED", "MACHINE", machine.getId(), "Cadastro de " + machine.getAssetCode() + " atualizado");
        return MachineResponse.from(machine);
    }

    @Transactional
    public MachineResponse updateStatus(UUID machineId, UpdateMachineStatusRequest request) {
        Machine machine = findMachine(machineId);
        boolean lifecycleChange = request.status() == MachineStatus.INACTIVE || machine.getStatus() == MachineStatus.INACTIVE;
        if (lifecycleChange && !currentUserIsAdmin()) {
            throw new AccessDeniedException("Somente administradores podem desativar ou reativar máquinas.");
        }
        machine.changeStatus(request.status());
        auditService.record("MACHINE_STATUS_CHANGED", "MACHINE", machine.getId(), "Status de " + machine.getAssetCode() + " alterado para " + request.status());
        return MachineResponse.from(machine);
    }

    private Machine findMachine(UUID machineId) {
        return machineRepository.findById(machineId)
                .orElseThrow(() -> new NotFoundException("Máquina não encontrada."));
    }

    private ProductionLine findAvailableProductionLine(UUID productionLineId) {
        ProductionLine line = productionLineRepository.findById(productionLineId)
                .orElseThrow(() -> new NotFoundException("Linha de produção não encontrada."));
        if (!line.isActive() || !line.getSector().isActive() || !line.getSector().getPlant().isActive()) {
            throw new ConflictException("A planta, o setor e a linha precisam estar ativos para receber máquinas.");
        }
        return line;
    }

    private boolean currentUserIsAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
