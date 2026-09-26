package com.linepulse.asset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

record CreatePlantRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 40) String code
) {
}

record UpdatePlantRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 40) String code
) {
}

record CreateSectorRequest(
        @NotNull UUID plantId,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 40) String code
) {
}

record UpdateSectorRequest(
        @NotNull UUID plantId,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 40) String code
) {
}

record CreateProductionLineRequest(
        @NotNull UUID sectorId,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 40) String code
) {
}

record UpdateProductionLineRequest(
        @NotNull UUID sectorId,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 40) String code
) {
}

record UpdateStructureStatusRequest(boolean active) {
}
