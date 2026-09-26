package com.linepulse.asset;

import java.time.LocalDate;
import java.util.UUID;

public record MachineResponse(
        UUID id,
        UUID productionLineId,
        String productionLine,
        UUID sectorId,
        String sector,
        UUID plantId,
        String plant,
        String name,
        String assetCode,
        String manufacturer,
        String model,
        String serialNumber,
        MachineStatus status,
        LocalDate installedAt
) {
    static MachineResponse from(Machine machine) {
        ProductionLine line = machine.getProductionLine();
        Sector sector = line.getSector();
        Plant plant = sector.getPlant();
        return new MachineResponse(
                machine.getId(),
                line.getId(),
                line.getName(),
                sector.getId(),
                sector.getName(),
                plant.getId(),
                plant.getName(),
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
