package com.linepulse.auth;

import com.linepulse.organization.OrganizationMembership;
import com.linepulse.organization.OrganizationRole;
import com.linepulse.organization.OrganizationType;
import java.util.Set;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String name,
        String registration,
        UserRole role,
        boolean active,
        boolean demoAccount
) {
    private static final Set<String> DEMO_REGISTRATIONS = Set.of("ADM001", "TEC001", "OPE001");

    static AdminUserResponse from(UserAccount user) {
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getRegistration(),
                user.getRole(),
                user.isActive(),
                DEMO_REGISTRATIONS.contains(user.getRegistration())
        );
    }

    static AdminUserResponse from(OrganizationMembership membership) {
        UserAccount user = membership.getUser();
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getRegistration(),
                legacyRoleFor(membership),
                user.isActive() && membership.isActive(),
                DEMO_REGISTRATIONS.contains(user.getRegistration())
        );
    }

    private static UserRole legacyRoleFor(OrganizationMembership membership) {
        if (membership.getRole() == OrganizationRole.OPERATOR) {
            return UserRole.OPERATOR;
        }
        if (membership.getOrganization().getType() == OrganizationType.SERVICE_PROVIDER) {
            return UserRole.TECHNICIAN;
        }
        return switch (membership.getRole()) {
            case OWNER, ADMIN -> UserRole.ADMIN;
            case TECHNICIAN, MECHANIC -> UserRole.TECHNICIAN;
            case OPERATOR -> UserRole.OPERATOR;
        };
    }
}
