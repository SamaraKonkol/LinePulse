package com.linepulse.asset;

import java.util.UUID;

public record ProductionLineResponse(
        UUID id,
        UUID sectorId,
        String sector,
        UUID plantId,
        String plant,
        String name,
        String code,
        boolean active
) {
    static ProductionLineResponse from(ProductionLine line) {
        Sector sector = line.getSector();
        Plant plant = sector.getPlant();
        return new ProductionLineResponse(
                line.getId(),
                sector.getId(),
                sector.getName(),
                plant.getId(),
                plant.getName(),
                line.getName(),
                line.getCode(),
                line.isActive()
        );
    }
}
