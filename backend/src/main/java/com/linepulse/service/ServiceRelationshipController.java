package com.linepulse.service;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/provider-network")
public class ServiceRelationshipController {
    private final ServiceRelationshipService relationshipService;

    public ServiceRelationshipController(ServiceRelationshipService relationshipService) {
        this.relationshipService = relationshipService;
    }

    @GetMapping("/providers")
    List<ProviderOrganizationResponse> providers() {
        return relationshipService.availableProviders();
    }

    @GetMapping("/relationships")
    List<ServiceRelationshipResponse> relationships() {
        return relationshipService.trustedProviders();
    }

    @PostMapping("/relationships")
    ServiceRelationshipResponse trust(@Valid @RequestBody CreateServiceRelationshipRequest request) {
        return relationshipService.trust(request);
    }

    @PatchMapping("/relationships/{id}/suspend")
    ServiceRelationshipResponse suspend(@PathVariable UUID id) {
        return relationshipService.suspend(id);
    }
}
