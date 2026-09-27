package com.linepulse.organization;

import java.util.UUID;

public record OrganizationMemberResponse(
        Long membershipId,
        UUID userId,
        String name,
        String registration,
        OrganizationRole role,
        boolean active
) {
    public static OrganizationMemberResponse from(OrganizationMembership membership) {
        return new OrganizationMemberResponse(
                membership.getId(),
                membership.getUser().getId(),
                membership.getUser().getName(),
                membership.getUser().getRegistration(),
                membership.getRole(),
                membership.isActive()
        );
    }
}
