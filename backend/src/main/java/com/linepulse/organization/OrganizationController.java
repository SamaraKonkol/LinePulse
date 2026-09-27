package com.linepulse.organization;

import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {
    private final OrganizationService organizationService;
    private final OrganizationAccessService accessService;

    public OrganizationController(OrganizationService organizationService, OrganizationAccessService accessService) {
        this.organizationService = organizationService;
        this.accessService = accessService;
    }

    @GetMapping("/my")
    List<OrganizationSummaryResponse> myOrganizations(Principal principal) {
        return organizationService.findForUser(principal.getName());
    }

    @GetMapping("/current")
    OrganizationSummaryResponse currentOrganization() {
        return OrganizationSummaryResponse.from(accessService.currentMembership());
    }
}
