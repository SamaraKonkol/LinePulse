package com.linepulse.asset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateMachineRequest(
        @NotNull UUID productionLineId,
        @NotBlank String name,
        @NotBlank String assetCode,
        String manufacturer,
        String model,
        String serialNumber,
        LocalDate installedAt
) {
}
