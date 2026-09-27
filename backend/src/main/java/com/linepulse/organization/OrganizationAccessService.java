package com.linepulse.organization;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class OrganizationAccessService {
    private final OrganizationMembershipRepository membershipRepository;

    public OrganizationAccessService(OrganizationMembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public OrganizationMembership currentMembership() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }
        String registration = authentication.getName();
        OrganizationMembership membership = requestedOrganizationId()
                .flatMap(id -> membershipRepository.findByOrganization_IdAndUser_RegistrationIgnoreCaseAndActiveTrue(id, registration))
                .orElseGet(() -> membershipRepository
                        .findFirstByUser_RegistrationIgnoreCaseAndActiveTrueOrderByCreatedAtAsc(registration)
                        .orElseThrow(() -> new AccessDeniedException("Nenhuma organização ativa disponível para este usuário.")));
        if (!membership.getOrganization().isActive()) {
            throw new AccessDeniedException("A organização selecionada está inativa.");
        }
        return membership;
    }

    @Transactional(readOnly = true)
    public OrganizationMembership requireCurrentRole(OrganizationRole... allowedRoles) {
        OrganizationMembership membership = currentMembership();
        if (Arrays.stream(allowedRoles).noneMatch(role -> role == membership.getRole())) {
            throw new AccessDeniedException("Seu papel nesta organização não permite esta ação.");
        }
        return membership;
    }

    @Transactional(readOnly = true)
    public OrganizationMembership requireMembership(UUID organizationId, String registration, OrganizationRole... allowedRoles) {
        OrganizationMembership membership = membershipRepository
                .findByOrganization_IdAndUser_RegistrationIgnoreCaseAndActiveTrue(organizationId, registration)
                .filter(candidate -> candidate.getOrganization().isActive())
                .orElseThrow(() -> new AccessDeniedException("Usuário não pertence à organização informada."));
        if (allowedRoles.length > 0 && Arrays.stream(allowedRoles).noneMatch(role -> role == membership.getRole())) {
            throw new AccessDeniedException("Seu papel nesta organização não permite esta ação.");
        }
        return membership;
    }

    private Optional<UUID> requestedOrganizationId() {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return Optional.empty();
        }
        String value = servletAttributes.getRequest().getHeader(OrganizationService.ORGANIZATION_HEADER);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("Organização selecionada inválida.");
        }
    }
}
