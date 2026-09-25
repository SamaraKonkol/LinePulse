package com.linepulse.asset;

import jakarta.validation.constraints.NotNull;

public record UpdateMachineStatusRequest(
        @NotNull MachineStatus status
) {
}
