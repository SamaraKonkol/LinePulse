package com.linepulse.organization;

import java.util.UUID;

public record OrganizationSummaryResponse(
        UUID id,
        String name,
        String slug,
        OrganizationType type,
        OrganizationRole role
) {
    static OrganizationSummaryResponse from(OrganizationMembership membership) {
        Organization organization = membership.getOrganization();
        return new OrganizationSummaryResponse(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getType(),
                membership.getRole()
        );
    }
}
