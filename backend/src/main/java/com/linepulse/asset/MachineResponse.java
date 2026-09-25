package com.linepulse.asset;

import java.time.LocalDate;
import java.util.UUID;

public record MachineResponse(
        UUID id,
        UUID productionLineId,
        String productionLine,
        String name,
        String assetCode,
        String manufacturer,
        String model,
        String serialNumber,
        MachineStatus status,
        LocalDate installedAt
) {
    static MachineResponse from(Machine machine) {
        return new MachineResponse(
                machine.getId(),
                machine.getProductionLine().getId(),
                machine.getProductionLine().getName(),
                machine.getName(),
                machine.getAssetCode(),
                machine.getManufacturer(),
                machine.getModel(),
                machine.getSerialNumber(),
                machine.getStatus(),
                machine.getInstalledAt()
        );
    }
}
