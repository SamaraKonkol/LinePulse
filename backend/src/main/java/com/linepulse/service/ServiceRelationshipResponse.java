package com.linepulse.service;

import java.time.Instant;
import java.util.UUID;

public record ServiceRelationshipResponse(
        UUID id,
        UUID companyOrganizationId,
        String companyName,
        UUID providerOrganizationId,
        String providerName,
        ServiceRelationshipStatus status,
        Instant createdAt
) {
    public static ServiceRelationshipResponse from(ServiceRelationship relationship) {
        return new ServiceRelationshipResponse(
                relationship.getId(),
                relationship.getCompany().getId(),
                relationship.getCompany().getName(),
                relationship.getProvider().getId(),
                relationship.getProvider().getName(),
                relationship.getStatus(),
                relationship.getCreatedAt()
        );
    }
}
