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
        if (machineRepository.existsByAssetCodeIgnoreCase(request.assetCode())) {
            throw new ConflictException("Asset code already exists");
        }

        ProductionLine line = productionLineRepository.findById(request.productionLineId())
                .orElseThrow(() -> new NotFoundException("Production line not found"));
        Instant now = Instant.now();
        Machine machine = new Machine(
                UUID.randomUUID(),
                line,
                request.name().trim(),
                request.assetCode().trim().toUpperCase(),
                request.manufacturer(),
                request.model(),
                request.serialNumber(),
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
    public MachineResponse updateStatus(UUID machineId, UpdateMachineStatusRequest request) {
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new NotFoundException("Machine not found"));
        machine.changeStatus(request.status());
        auditService.record("MACHINE_STATUS_CHANGED", "MACHINE", machine.getId(), "Status de " + machine.getAssetCode() + " alterado para " + request.status());
        return MachineResponse.from(machine);
    }
}
