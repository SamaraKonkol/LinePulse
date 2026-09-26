package com.linepulse.asset;

import java.util.UUID;

public record SectorResponse(
        UUID id,
        UUID plantId,
        String plant,
        String name,
        String code,
        boolean active
) {
    static SectorResponse from(Sector sector) {
        return new SectorResponse(
                sector.getId(),
                sector.getPlant().getId(),
                sector.getPlant().getName(),
                sector.getName(),
                sector.getCode(),
                sector.isActive()
        );
    }
}
