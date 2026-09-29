package com.linepulse.organization;

import com.linepulse.auth.UserAccount;
import com.linepulse.auth.UserRole;
import com.linepulse.common.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class OrganizationService {
    public static final String DEFAULT_SLUG = "linepulse-default";
    public static final String ORGANIZATION_HEADER = "X-LinePulse-Organization";

    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;

    public OrganizationService(OrganizationRepository organizationRepository, OrganizationMembershipRepository membershipRepository) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public List<OrganizationSummaryResponse> findForUser(String registration) {
        return membershipRepository.findByUser_RegistrationIgnoreCaseAndActiveTrueOrderByOrganization_NameAsc(registration).stream()
                .map(OrganizationSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Organization currentOrganization() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }
        String registration = authentication.getName();
        Optional<UUID> requestedOrganization = requestedOrganizationId();
        OrganizationMembership membership = requestedOrganization.isPresent()
                ? membershipRepository.findByOrganization_IdAndUser_RegistrationIgnoreCaseAndActiveTrue(requestedOrganization.get(), registration)
                        .orElseThrow(() -> new AccessDeniedException("Usuário não pertence ao workspace selecionado."))
                : membershipRepository.findFirstByUser_RegistrationIgnoreCaseAndActiveTrueOrderByCreatedAtAsc(registration)
                        .orElseThrow(() -> new AccessDeniedException("Nenhuma organização ativa disponível para este usuário."));
        if (!membership.getOrganization().isActive()) {
            throw new AccessDeniedException("A organização selecionada está inativa.");
        }
        return membership.getOrganization();
    }

    @Transactional
    public void ensureDefaultMembership(UserAccount user) {
        if (membershipRepository.existsByUser_RegistrationIgnoreCase(user.getRegistration())) {
            return;
        }
        membershipRepository.save(new OrganizationMembership(
                defaultOrganization(),
                user,
                mapRole(user.getRole()),
                true,
                Instant.now()
        ));
    }

    @Transactional
    public void inheritPrimaryOrganization(String actorRegistration, UserAccount newUser) {
        Organization organization = requestedOrganizationId()
                .flatMap(id -> membershipRepository.findByOrganization_IdAndUser_RegistrationIgnoreCaseAndActiveTrue(id, actorRegistration))
                .map(OrganizationMembership::getOrganization)
                .orElseGet(() -> membershipRepository
                        .findFirstByUser_RegistrationIgnoreCaseAndActiveTrueOrderByCreatedAtAsc(actorRegistration)
                        .map(OrganizationMembership::getOrganization)
                        .orElseGet(this::defaultOrganization));

        if (!membershipRepository.existsByOrganization_IdAndUser_Id(organization.getId(), newUser.getId())) {
            membershipRepository.save(new OrganizationMembership(
                    organization,
                    newUser,
                    mapRole(newUser.getRole()),
                    true,
                    Instant.now()
            ));
        }
    }

    private Optional<UUID> requestedOrganizationId() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return Optional.empty();
        }
        String value = servletAttributes.getRequest().getHeader(ORGANIZATION_HEADER);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("Organização selecionada inválida.");
        }
    }

    private Organization defaultOrganization() {
        return organizationRepository.findBySlugIgnoreCase(DEFAULT_SLUG)
                .orElseThrow(() -> new NotFoundException("Organização padrão não encontrada."));
    }

    private OrganizationRole mapRole(UserRole role) {
        return switch (role) {
            case ADMIN -> OrganizationRole.OWNER;
            case TECHNICIAN -> OrganizationRole.TECHNICIAN;
            case OPERATOR -> OrganizationRole.OPERATOR;
        };
    }
}
