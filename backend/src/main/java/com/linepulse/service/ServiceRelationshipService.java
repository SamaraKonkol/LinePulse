package com.linepulse.service;

import com.linepulse.common.ConflictException;
import com.linepulse.common.NotFoundException;
import com.linepulse.organization.Organization;
import com.linepulse.organization.OrganizationAccessService;
import com.linepulse.organization.OrganizationRepository;
import com.linepulse.organization.OrganizationRole;
import com.linepulse.organization.OrganizationType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceRelationshipService {
    private final OrganizationAccessService accessService;
    private final OrganizationRepository organizationRepository;
    private final ServiceRelationshipRepository relationshipRepository;

    public ServiceRelationshipService(OrganizationAccessService accessService, OrganizationRepository organizationRepository, ServiceRelationshipRepository relationshipRepository) {
        this.accessService = accessService;
        this.organizationRepository = organizationRepository;
        this.relationshipRepository = relationshipRepository;
    }

    @Transactional(readOnly = true)
    public List<ProviderOrganizationResponse> availableProviders() {
        var membership = accessService.currentMembership();
        if (membership.getOrganization().getType() != OrganizationType.COMPANY) {
            throw new org.springframework.security.access.AccessDeniedException("Somente empresas podem consultar prestadores.");
        }
        return organizationRepository.findByTypeAndActiveTrueOrderByNameAsc(OrganizationType.SERVICE_PROVIDER).stream()
                .map(ProviderOrganizationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceRelationshipResponse> trustedProviders() {
        var membership = accessService.currentMembership();
        if (membership.getOrganization().getType() != OrganizationType.COMPANY) {
            throw new org.springframework.security.access.AccessDeniedException("Somente empresas possuem uma rede de prestadores.");
        }
        return relationshipRepository.findByCompany_IdOrderByCreatedAtDesc(membership.getOrganization().getId()).stream()
                .map(ServiceRelationshipResponse::from)
                .toList();
    }

    @Transactional
    public ServiceRelationshipResponse trust(CreateServiceRelationshipRequest request) {
        var membership = accessService.requireCurrentRole(OrganizationRole.OWNER, OrganizationRole.ADMIN);
        Organization company = membership.getOrganization();
        if (company.getType() != OrganizationType.COMPANY) {
            throw new org.springframework.security.access.AccessDeniedException("Somente empresas podem vincular prestadores.");
        }
        Organization provider = organizationRepository.findById(request.providerOrganizationId())
                .filter(Organization::isActive)
                .filter(candidate -> candidate.getType() == OrganizationType.SERVICE_PROVIDER)
                .orElseThrow(() -> new NotFoundException("Prestador não encontrado."));
        if (company.getId().equals(provider.getId())) {
            throw new ConflictException("Uma organização não pode ser prestadora de si mesma.");
        }
        Instant now = Instant.now();
        ServiceRelationship relationship = relationshipRepository.findByCompany_IdAndProvider_Id(company.getId(), provider.getId())
                .map(existing -> {
                    existing.activate(now);
                    return existing;
                })
                .orElseGet(() -> new ServiceRelationship(UUID.randomUUID(), company, provider, ServiceRelationshipStatus.ACTIVE, now, now));
        return ServiceRelationshipResponse.from(relationshipRepository.save(relationship));
    }

    @Transactional
    public ServiceRelationshipResponse suspend(UUID relationshipId) {
        var membership = accessService.requireCurrentRole(OrganizationRole.OWNER, OrganizationRole.ADMIN);
        ServiceRelationship relationship = relationshipRepository.findByIdAndCompany_Id(relationshipId, membership.getOrganization().getId())
                .orElseThrow(() -> new NotFoundException("Vínculo com prestador não encontrado."));
        relationship.suspend(Instant.now());
        return ServiceRelationshipResponse.from(relationshipRepository.save(relationship));
    }
}
