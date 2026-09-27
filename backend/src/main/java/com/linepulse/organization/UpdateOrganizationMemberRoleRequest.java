package com.linepulse.organization;

import jakarta.validation.constraints.NotNull;

public record UpdateOrganizationMemberRoleRequest(@NotNull OrganizationRole role) {
}
