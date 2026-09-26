package com.linepulse.asset;

import java.util.UUID;

public record ProductionLineResponse(
        UUID id,
        String name,
        String code,
        boolean active
) {
    static ProductionLineResponse from(ProductionLine line) {
        return new ProductionLineResponse(line.getId(), line.getName(), line.getCode(), line.isActive());
    }
}
