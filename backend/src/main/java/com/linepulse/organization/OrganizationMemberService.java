package com.linepulse.organization;

import com.linepulse.audit.AuditService;
import com.linepulse.auth.UserAccount;
import com.linepulse.auth.UserRepository;
import com.linepulse.auth.UserRole;
import com.linepulse.common.ConflictException;
import com.linepulse.common.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationMemberService {
    private final OrganizationAccessService accessService;
    private final OrganizationMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public OrganizationMemberService(
            OrganizationAccessService accessService,
            OrganizationMembershipRepository membershipRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService
    ) {
        this.accessService = accessService;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<OrganizationMemberResponse> list() {
        var current = accessService.requireCurrentRole(OrganizationRole.OWNER, OrganizationRole.ADMIN, OrganizationRole.TECHNICIAN, OrganizationRole.MECHANIC);
        return membershipRepository.findByOrganization_IdOrderByUser_NameAsc(current.getOrganization().getId()).stream()
                .map(OrganizationMemberResponse::from)
                .toList();
    }

    @Transactional
    public OrganizationMemberResponse create(CreateOrganizationMemberRequest request) {
        var actor = accessService.requireCurrentRole(OrganizationRole.OWNER, OrganizationRole.ADMIN);
        Organization organization = actor.getOrganization();
        if (request.role() == OrganizationRole.OWNER && actor.getRole() != OrganizationRole.OWNER) {
            throw new AccessDeniedException("Apenas um OWNER pode conceder propriedade da organização.");
        }
        if (userRepository.existsByRegistrationIgnoreCase(request.registration())) {
            throw new ConflictException("Já existe uma conta com este cadastro.");
        }
        Instant now = Instant.now();
        UserAccount user = userRepository.save(new UserAccount(
                UUID.randomUUID(), request.name().trim(), request.registration().trim(), passwordEncoder.encode(request.password()),
                globalRoleFor(organization, request.role()), true, now, now
        ));
        OrganizationMembership membership = membershipRepository.save(new OrganizationMembership(organization, user, request.role(), true, now));
        auditService.recordForOrganization(organization, "ORGANIZATION_MEMBER_CREATED", "USER", user.getId(), "Membro " + user.getRegistration() + " criado como " + request.role());
        return OrganizationMemberResponse.from(membership);
    }

    @Transactional
    public OrganizationMemberResponse changeRole(UUID userId, UpdateOrganizationMemberRoleRequest request) {
        var actor = accessService.requireCurrentRole(OrganizationRole.OWNER, OrganizationRole.ADMIN);
        OrganizationMembership membership = findMembership(actor.getOrganization().getId(), userId);
        ensureOwnerManagementAllowed(actor, membership, request.role());
        ensureOrganizationKeepsActiveOwner(actor.getOrganization().getId(), membership, request.role(), membership.isActive());
        membership.changeRole(request.role());
        membership.getUser().changeRole(globalRoleFor(actor.getOrganization(), request.role()));
        userRepository.save(membership.getUser());
        OrganizationMembership saved = membershipRepository.save(membership);
        auditService.recordForOrganization(actor.getOrganization(), "ORGANIZATION_MEMBER_ROLE_CHANGED", "USER", userId, "Papel de " + saved.getUser().getRegistration() + " alterado para " + request.role());
        return OrganizationMemberResponse.from(saved);
    }

    @Transactional
    public OrganizationMemberResponse changeStatus(UUID userId, UpdateOrganizationMemberStatusRequest request) {
        var actor = accessService.requireCurrentRole(OrganizationRole.OWNER, OrganizationRole.ADMIN);
        OrganizationMembership membership = findMembership(actor.getOrganization().getId(), userId);
        if (membership.getRole() == OrganizationRole.OWNER && actor.getRole() != OrganizationRole.OWNER) {
            throw new AccessDeniedException("Apenas um OWNER pode alterar o status de outro OWNER.");
        }
        ensureOrganizationKeepsActiveOwner(actor.getOrganization().getId(), membership, membership.getRole(), request.active());
        membership.changeActive(request.active());
        OrganizationMembership saved = membershipRepository.save(membership);
        auditService.recordForOrganization(actor.getOrganization(), "ORGANIZATION_MEMBER_STATUS_CHANGED", "USER", userId, "Membro " + saved.getUser().getRegistration() + (request.active() ? " ativado" : " desativado"));
        return OrganizationMemberResponse.from(saved);
    }

    private void ensureOwnerManagementAllowed(OrganizationMembership actor, OrganizationMembership target, OrganizationRole resultingRole) {
        boolean touchesOwnership = target.getRole() == OrganizationRole.OWNER || resultingRole == OrganizationRole.OWNER;
        if (touchesOwnership && actor.getRole() != OrganizationRole.OWNER) {
            throw new AccessDeniedException("Apenas um OWNER pode conceder ou remover propriedade da organização.");
        }
    }

    private void ensureOrganizationKeepsActiveOwner(UUID organizationId, OrganizationMembership membership, OrganizationRole resultingRole, boolean resultingActive) {
        boolean removesActiveOwner = membership.isActive()
                && membership.getRole() == OrganizationRole.OWNER
                && (!resultingActive || resultingRole != OrganizationRole.OWNER);
        if (removesActiveOwner
                && membershipRepository.countByOrganization_IdAndRoleAndActiveTrue(organizationId, OrganizationRole.OWNER) <= 1) {
            throw new ConflictException("A organização precisa manter pelo menos um OWNER ativo.");
        }
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
