package com.linepulse.service;

import com.linepulse.audit.AuditService;
import com.linepulse.auth.UserAccount;
import com.linepulse.auth.UserRepository;
import com.linepulse.auth.UserRole;
import com.linepulse.common.ConflictException;
import com.linepulse.common.NotFoundException;
import com.linepulse.organization.Organization;
import com.linepulse.organization.OrganizationAccessService;
import com.linepulse.organization.OrganizationMembership;
import com.linepulse.organization.OrganizationMembershipRepository;
import com.linepulse.organization.OrganizationRepository;
import com.linepulse.organization.OrganizationRole;
import com.linepulse.organization.OrganizationType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceRelationshipService {
    private final OrganizationAccessService accessService;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final ServiceRelationshipRepository relationshipRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public ServiceRelationshipService(
            OrganizationAccessService accessService,
            OrganizationRepository organizationRepository,
            OrganizationMembershipRepository membershipRepository,
            ServiceRelationshipRepository relationshipRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService
    ) {
        this.accessService = accessService;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.relationshipRepository = relationshipRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ProviderOrganizationResponse> availableProviders() {
        var membership = accessService.currentMembership();
        if (membership.getOrganization().getType() != OrganizationType.COMPANY) {
            throw new AccessDeniedException("Somente empresas podem consultar prestadores.");
        }
        return organizationRepository.findByTypeAndActiveTrueOrderByNameAsc(OrganizationType.SERVICE_PROVIDER).stream()
                .map(ProviderOrganizationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceRelationshipResponse> trustedProviders() {
        var membership = accessService.currentMembership();
        if (membership.getOrganization().getType() != OrganizationType.COMPANY) {
            throw new AccessDeniedException("Somente empresas possuem uma rede de prestadores.");
        }
        return relationshipRepository.findByCompany_IdOrderByCreatedAtDesc(membership.getOrganization().getId()).stream()
                .map(ServiceRelationshipResponse::from).toList();
    }

    @Transactional
    public ProviderOnboardingResponse onboardProvider(CreateProviderOrganizationRequest request) {
        var actor = accessService.requireCurrentRole(OrganizationRole.OWNER, OrganizationRole.ADMIN);
        Organization company = actor.getOrganization();
        if (company.getType() != OrganizationType.COMPANY) throw new AccessDeniedException("Somente empresas podem cadastrar um novo prestador.");
        if (organizationRepository.findBySlugIgnoreCase(request.slug()).isPresent()) throw new ConflictException("Já existe uma organização com este identificador.");
        if (userRepository.existsByRegistrationIgnoreCase(request.ownerRegistration())) throw new ConflictException("Já existe uma conta com o cadastro do responsável informado.");

        Instant now = Instant.now();
        Organization provider = organizationRepository.save(new Organization(UUID.randomUUID(), request.name().trim(), request.slug().trim().toLowerCase(), OrganizationType.SERVICE_PROVIDER, true, now));
        UserAccount owner = userRepository.save(new UserAccount(UUID.randomUUID(), request.ownerName().trim(), request.ownerRegistration().trim(), passwordEncoder.encode(request.ownerPassword()), UserRole.TECHNICIAN, true, now, now));
        membershipRepository.save(new OrganizationMembership(provider, owner, OrganizationRole.OWNER, true, now));
        ServiceRelationship relationship = relationshipRepository.save(new ServiceRelationship(UUID.randomUUID(), company, provider, ServiceRelationshipStatus.ACTIVE, now, now));
        auditService.recordForOrganization(company, "SERVICE_PROVIDER_ONBOARDED", "ORGANIZATION", provider.getId(), "Prestador " + provider.getName() + " criado e vinculado");
        auditService.recordForOrganization(provider, "SERVICE_PROVIDER_ONBOARDED", "ORGANIZATION", provider.getId(), "Organização prestadora criada e vinculada a " + company.getName());
        return new ProviderOnboardingResponse(ProviderOrganizationResponse.from(provider), ServiceRelationshipResponse.from(relationship), owner.getRegistration());
    }

    @Transactional
    public ServiceRelationshipResponse trust(CreateServiceRelationshipRequest request) {
        var membership = accessService.requireCurrentRole(OrganizationRole.OWNER, OrganizationRole.ADMIN);
        Organization company = membership.getOrganization();
        if (company.getType() != OrganizationType.COMPANY) throw new AccessDeniedException("Somente empresas podem vincular prestadores.");
        Organization provider = organizationRepository.findById(request.providerOrganizationId())
                .filter(Organization::isActive)
                .filter(candidate -> candidate.getType() == OrganizationType.SERVICE_PROVIDER)
                .orElseThrow(() -> new NotFoundException("Prestador não encontrado."));
        if (company.getId().equals(provider.getId())) throw new ConflictException("Uma organização não pode ser prestadora de si mesma.");
        Instant now = Instant.now();
        ServiceRelationship relationship = relationshipRepository.findByCompany_IdAndProvider_Id(company.getId(), provider.getId())
                .map(existing -> { existing.activate(now); return existing; })
                .orElseGet(() -> new ServiceRelationship(UUID.randomUUID(), company, provider, ServiceRelationshipStatus.ACTIVE, now, now));
        ServiceRelationship saved = relationshipRepository.save(relationship);
        auditService.recordForOrganization(company, "SERVICE_PROVIDER_TRUSTED", "ORGANIZATION", provider.getId(), "Prestador " + provider.getName() + " ativado na rede");
        auditService.recordForOrganization(provider, "SERVICE_PROVIDER_TRUSTED", "ORGANIZATION", company.getId(), "Empresa " + company.getName() + " ativou vínculo de atendimento");
        return ServiceRelationshipResponse.from(saved);
    }

    @Transactional
    public ServiceRelationshipResponse suspend(UUID relationshipId) {
        var membership = accessService.requireCurrentRole(OrganizationRole.OWNER, OrganizationRole.ADMIN);
        ServiceRelationship relationship = relationshipRepository.findByIdAndCompany_Id(relationshipId, membership.getOrganization().getId())
                .orElseThrow(() -> new NotFoundException("Vínculo com prestador não encontrado."));
        relationship.suspend(Instant.now());
        ServiceRelationship saved = relationshipRepository.save(relationship);
        auditService.recordForOrganization(saved.getCompany(), "SERVICE_PROVIDER_SUSPENDED", "ORGANIZATION", saved.getProvider().getId(), "Prestador " + saved.getProvider().getName() + " suspenso na rede");
        auditService.recordForOrganization(saved.getProvider(), "SERVICE_PROVIDER_SUSPENDED", "ORGANIZATION", saved.getCompany().getId(), "Vínculo com " + saved.getCompany().getName() + " suspenso");
        return ServiceRelationshipResponse.from(saved);
    }
}
