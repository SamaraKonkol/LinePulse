package com.linepulse.organization;

import com.linepulse.auth.UserAccount;
import com.linepulse.auth.UserRepository;
import com.linepulse.auth.UserRole;
import com.linepulse.common.ConflictException;
import com.linepulse.common.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationMemberService {
    private final OrganizationAccessService accessService;
    private final OrganizationMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public OrganizationMemberService(
            OrganizationAccessService accessService,
            OrganizationMembershipRepository membershipRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.accessService = accessService;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<OrganizationMemberResponse> list() {
        var current = accessService.currentMembership();
        return membershipRepository.findByOrganization_IdOrderByUser_NameAsc(current.getOrganization().getId()).stream()
                .map(OrganizationMemberResponse::from)
                .toList();
    }

    @Transactional
    public OrganizationMemberResponse create(CreateOrganizationMemberRequest request) {
        var actor = accessService.requireCurrentRole(OrganizationRole.OWNER, OrganizationRole.ADMIN);
        Organization organization = actor.getOrganization();
        if (userRepository.existsByRegistrationIgnoreCase(request.registration())) {
            throw new ConflictException("Já existe uma conta com este cadastro.");
        }
        Instant now = Instant.now();
        UserAccount user = userRepository.save(new UserAccount(
                UUID.randomUUID(),
                request.name().trim(),
                request.registration().trim(),
                passwordEncoder.encode(request.password()),
                globalRoleFor(organization, request.role()),
                true,
                now,
                now
        ));
        OrganizationMembership membership = membershipRepository.save(new OrganizationMembership(
                organization,
                user,
                request.role(),
                true,
                now
        ));
        return OrganizationMemberResponse.from(membership);
    }

    @Transactional
    public OrganizationMemberResponse changeRole(UUID userId, UpdateOrganizationMemberRoleRequest request) {
        var actor = accessService.requireCurrentRole(OrganizationRole.OWNER, OrganizationRole.ADMIN);
        OrganizationMembership membership = findMembership(actor.getOrganization().getId(), userId);
        membership.changeRole(request.role());
        membership.getUser().changeRole(globalRoleFor(actor.getOrganization(), request.role()));
        userRepository.save(membership.getUser());
        return OrganizationMemberResponse.from(membershipRepository.save(membership));
    }

    @Transactional
    public OrganizationMemberResponse changeStatus(UUID userId, UpdateOrganizationMemberStatusRequest request) {
        var actor = accessService.requireCurrentRole(OrganizationRole.OWNER, OrganizationRole.ADMIN);
        OrganizationMembership membership = findMembership(actor.getOrganization().getId(), userId);
        membership.changeActive(request.active());
        return OrganizationMemberResponse.from(membershipRepository.save(membership));
    }

    private OrganizationMembership findMembership(UUID organizationId, UUID userId) {
        return membershipRepository.findByOrganization_IdAndUser_Id(organizationId, userId)
                .orElseThrow(() -> new NotFoundException("Membro não encontrado nesta organização."));
    }

    private UserRole globalRoleFor(Organization organization, OrganizationRole organizationRole) {
        if (organization.getType() == OrganizationType.SERVICE_PROVIDER) {
            return organizationRole == OrganizationRole.OPERATOR ? UserRole.OPERATOR : UserRole.TECHNICIAN;
        }
        return switch (organizationRole) {
            case OWNER, ADMIN -> UserRole.ADMIN;
            case TECHNICIAN, MECHANIC -> UserRole.TECHNICIAN;
            case OPERATOR -> UserRole.OPERATOR;
        };
    }
}
