package com.linepulse.service;

public record ProviderOnboardingResponse(
        ProviderOrganizationResponse provider,
        ServiceRelationshipResponse relationship,
        String ownerRegistration
) {
}
