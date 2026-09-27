package com.linepulse.organization;

import java.util.Arrays;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return membershipRepository
                .findFirstByUser_RegistrationIgnoreCaseAndActiveTrueOrderByCreatedAtAsc(authentication.getName())
                .filter(membership -> membership.getOrganization().isActive())
                .orElseThrow(() -> new AccessDeniedException("Nenhuma organização ativa disponível para este usuário."));
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
}
