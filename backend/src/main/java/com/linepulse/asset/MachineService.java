package com.linepulse.asset;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MachineService {
    private final MachineRepository machineRepository;
    private final ProductionLineRepository productionLineRepository;

    public MachineService(MachineRepository machineRepository, ProductionLineRepository productionLineRepository) {
        this.machineRepository = machineRepository;
        this.productionLineRepository = productionLineRepository;
    }

    @Transactional(readOnly = true)
    public List<MachineResponse> findAll() {
        return machineRepository.findAll().stream().map(MachineResponse::from).toList();
    }

    @Transactional
    public MachineResponse create(CreateMachineRequest request) {
        if (machineRepository.existsByAssetCodeIgnoreCase(request.assetCode())) {
            throw new IllegalArgumentException("Asset code already exists");
        }

        ProductionLine line = productionLineRepository.findById(request.productionLineId())
                .orElseThrow(() -> new IllegalArgumentException("Production line not found"));
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
        return MachineResponse.from(machineRepository.save(machine));
    }
}
