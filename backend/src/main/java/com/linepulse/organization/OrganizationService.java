package com.linepulse.organization;

import com.linepulse.auth.UserAccount;
import com.linepulse.auth.UserRole;
import com.linepulse.common.NotFoundException;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {
    public static final String DEFAULT_SLUG = "linepulse-default";

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

    @Transactional
    public void ensureDefaultMembership(UserAccount user) {
        if (!membershipRepository.findByUser_RegistrationIgnoreCaseAndActiveTrueOrderByOrganization_NameAsc(user.getRegistration()).isEmpty()) {
            return;
        }
        Organization organization = defaultOrganization();
        membershipRepository.save(new OrganizationMembership(
                organization,
                user,
                mapRole(user.getRole()),
                true,
                Instant.now()
        ));
    }

    @Transactional
    public void inheritPrimaryOrganization(String actorRegistration, UserAccount newUser) {
        OrganizationMembership actorMembership = membershipRepository
                .findFirstByUser_RegistrationIgnoreCaseAndActiveTrueOrderByCreatedAtAsc(actorRegistration)
                .orElseGet(() -> {
                    Organization defaultOrganization = defaultOrganization();
                    return new OrganizationMembership(defaultOrganization, newUser, mapRole(newUser.getRole()), true, Instant.now());
                });

        Organization organization = actorMembership.getOrganization();
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
