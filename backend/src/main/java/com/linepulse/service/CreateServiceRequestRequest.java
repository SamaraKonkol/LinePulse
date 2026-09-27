package com.linepulse.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateServiceRequestRequest(
        @NotNull UUID machineId,
        UUID incidentId,
        @NotBlank String title,
        @NotBlank String description,
        @NotNull ServiceRequestChannel channel,
        @NotNull ServiceRequestPriority priority,
        UUID providerOrganizationId
) {
}
