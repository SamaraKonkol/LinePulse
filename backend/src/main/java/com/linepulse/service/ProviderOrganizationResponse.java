package com.linepulse.service;

import com.linepulse.organization.Organization;
import java.util.UUID;

public record ProviderOrganizationResponse(UUID id, String name, String slug) {
    public static ProviderOrganizationResponse from(Organization organization) {
        return new ProviderOrganizationResponse(organization.getId(), organization.getName(), organization.getSlug());
    }
}
