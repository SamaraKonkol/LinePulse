package com.linepulse.service;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateServiceRelationshipRequest(@NotNull UUID providerOrganizationId) {
}
