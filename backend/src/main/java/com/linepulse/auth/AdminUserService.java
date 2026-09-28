package com.linepulse.auth;

import com.linepulse.common.ConflictException;
import com.linepulse.organization.CreateOrganizationMemberRequest;
import com.linepulse.organization.OrganizationMemberService;
import com.linepulse.organization.OrganizationMembership;
import com.linepulse.organization.OrganizationMembershipRepository;
import com.linepulse.organization.OrganizationRole;
import com.linepulse.organization.OrganizationService;
import com.linepulse.organization.UpdateOrganizationMemberRoleRequest;
import com.linepulse.organization.UpdateOrganizationMemberStatusRequest;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {
    private static final Set<String> DEMO_REGISTRATIONS = Set.of("ADM001", "TEC001", "OPE001");

    private final OrganizationService organizationService;
    private final OrganizationMembershipRepository membershipRepository;
    private final OrganizationMemberService organizationMemberService;

    public AdminUserService(
            OrganizationService organizationService,
            OrganizationMembershipRepository membershipRepository,
            OrganizationMemberService organizationMemberService
    ) {
        this.organizationService = organizationService;
        this.membershipRepository = membershipRepository;
        this.organizationMemberService = organizationMemberService;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> findAll() {
        UUID organizationId = organizationService.currentOrganization().getId();
        return membershipRepository.findByOrganization_IdOrderByUser_NameAsc(organizationId).stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional
    public AdminUserResponse create(AdminCreateUserRequest request, String actorRegistration) {
        String registration = normalizeRegistration(request.registration());
        var created = organizationMemberService.create(new CreateOrganizationMemberRequest(
                request.name(),
                registration,
                request.password(),
                organizationRoleFor(request.role())
        ));
        return findResponse(created.userId());
    }

    @Transactional
    public AdminUserResponse updateRole(UUID userId, UpdateUserRoleRequest request, String actorRegistration) {
        OrganizationMembership membership = findMembership(userId);
        validateEditable(membership, actorRegistration);
        organizationMemberService.changeRole(userId, new UpdateOrganizationMemberRoleRequest(organizationRoleFor(request.role())));
        return findResponse(userId);
    }

    @Transactional
    public AdminUserResponse updateStatus(UUID userId, UpdateUserStatusRequest request, String actorRegistration) {
        OrganizationMembership membership = findMembership(userId);
        validateEditable(membership, actorRegistration);
        organizationMemberService.changeStatus(userId, new UpdateOrganizationMemberStatusRequest(request.active()));
        return findResponse(userId);
    }

    @Transactional
    public void delete(UUID userId, String actorRegistration) {
        OrganizationMembership membership = findMembership(userId);
        validateEditable(membership, actorRegistration);
        organizationMemberService.changeStatus(userId, new UpdateOrganizationMemberStatusRequest(false));
    }

    private OrganizationMembership findMembership(UUID userId) {
        UUID organizationId = organizationService.currentOrganization().getId();
        return membershipRepository.findByOrganization_IdAndUser_Id(organizationId, userId)
                .orElseThrow(() -> new com.linepulse.common.NotFoundException("Usuário não encontrado nesta organização."));
    }

    private AdminUserResponse findResponse(UUID userId) {
        return AdminUserResponse.from(findMembership(userId));
    }

    private void validateEditable(OrganizationMembership membership, String actorRegistration) {
        String registration = membership.getUser().getRegistration();
        if (registration.equalsIgnoreCase(actorRegistration)) {
            throw new ConflictException("Você não pode alterar ou remover o próprio acesso administrativo.");
        }
        if (DEMO_REGISTRATIONS.contains(registration)) {
            throw new ConflictException("As contas demo possuem acesso fixo e não podem ser alteradas.");
        }
    }

    private String normalizeRegistration(String registration) {
        return registration.trim().toUpperCase(Locale.ROOT);
    }

    private OrganizationRole organizationRoleFor(UserRole role) {
        return switch (role) {
            case ADMIN -> OrganizationRole.ADMIN;
            case TECHNICIAN -> OrganizationRole.TECHNICIAN;
            case OPERATOR -> OrganizationRole.OPERATOR;
        };
    }
}
