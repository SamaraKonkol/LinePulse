package com.linepulse.asset;

import com.linepulse.audit.AuditService;
import com.linepulse.common.ConflictException;
import com.linepulse.common.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
        return machineRepository.findAll().stream().map(MachineResponse::from).toList();
    }

    @Transactional
    public MachineResponse create(CreateMachineRequest request) {
        String assetCode = request.assetCode().trim().toUpperCase();
        if (machineRepository.existsByAssetCodeIgnoreCase(assetCode)) {
            throw new ConflictException("Asset code already exists");
        }

        ProductionLine line = findProductionLine(request.productionLineId());
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
            throw new ConflictException("Asset code already exists");
        }

        ProductionLine line = findProductionLine(request.productionLineId());
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
        machine.changeStatus(request.status());
        auditService.record("MACHINE_STATUS_CHANGED", "MACHINE", machine.getId(), "Status de " + machine.getAssetCode() + " alterado para " + request.status());
        return MachineResponse.from(machine);
    }

    private Machine findMachine(UUID machineId) {
        return machineRepository.findById(machineId)
                .orElseThrow(() -> new NotFoundException("Machine not found"));
    }

    private ProductionLine findProductionLine(UUID productionLineId) {
        return productionLineRepository.findById(productionLineId)
                .orElseThrow(() -> new NotFoundException("Production line not found"));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
