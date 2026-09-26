package com.linepulse.asset;

import java.util.UUID;

public record PlantResponse(
        UUID id,
        String name,
        String code,
        boolean active
) {
    static PlantResponse from(Plant plant) {
        return new PlantResponse(plant.getId(), plant.getName(), plant.getCode(), plant.isActive());
    }
}
