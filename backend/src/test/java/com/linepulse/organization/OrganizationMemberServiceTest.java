package com.linepulse.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.linepulse.audit.AuditService;
import com.linepulse.auth.UserAccount;
import com.linepulse.auth.UserRepository;
import com.linepulse.auth.UserRole;
import com.linepulse.common.ConflictException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class OrganizationMemberServiceTest {
    private OrganizationAccessService accessService;
    private OrganizationMembershipRepository membershipRepository;
    private UserRepository userRepository;
    private AuditService auditService;
    private OrganizationMemberService service;
    private Organization organization;
    private UserAccount ownerUser;
    private OrganizationMembership ownerMembership;

    @BeforeEach
    void setUp() {
        accessService = mock(OrganizationAccessService.class);
        membershipRepository = mock(OrganizationMembershipRepository.class);
        userRepository = mock(UserRepository.class);
        auditService = mock(AuditService.class);
        service = new OrganizationMemberService(
                accessService,
                membershipRepository,
                userRepository,
                mock(PasswordEncoder.class),
                auditService
        );

        Instant now = Instant.now();
        organization = new Organization(UUID.randomUUID(), "Empresa Teste", "empresa-teste", OrganizationType.COMPANY, true, now);
        ownerUser = new UserAccount(UUID.randomUUID(), "Owner Teste", "OWN001", "hash", UserRole.ADMIN, true, now, now);
        ownerMembership = new OrganizationMembership(organization, ownerUser, OrganizationRole.OWNER, true, now);

        when(accessService.requireCurrentRole(OrganizationRole.OWNER, OrganizationRole.ADMIN)).thenReturn(ownerMembership);
        when(membershipRepository.findByOrganization_IdAndUser_Id(organization.getId(), ownerUser.getId())).thenReturn(Optional.of(ownerMembership));
        when(membershipRepository.save(any(OrganizationMembership.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldRejectDemotingTheOnlyActiveOwner() {
        when(membershipRepository.countByOrganization_IdAndRoleAndActiveTrue(organization.getId(), OrganizationRole.OWNER)).thenReturn(1L);

        ConflictException error = assertThrows(
                ConflictException.class,
                () -> service.changeRole(ownerUser.getId(), new UpdateOrganizationMemberRoleRequest(OrganizationRole.ADMIN))
        );

        assertEquals("A organização precisa manter pelo menos um OWNER ativo.", error.getMessage());
        assertEquals(OrganizationRole.OWNER, ownerMembership.getRole());
        verify(membershipRepository, never()).save(any(OrganizationMembership.class));
    }

    @Test
    void shouldRejectDeactivatingTheOnlyActiveOwner() {
        when(membershipRepository.countByOrganization_IdAndRoleAndActiveTrue(organization.getId(), OrganizationRole.OWNER)).thenReturn(1L);

        ConflictException error = assertThrows(
                ConflictException.class,
                () -> service.changeStatus(ownerUser.getId(), new UpdateOrganizationMemberStatusRequest(false))
        );

        assertEquals("A organização precisa manter pelo menos um OWNER ativo.", error.getMessage());
        assertEquals(true, ownerMembership.isActive());
        verify(membershipRepository, never()).save(any(OrganizationMembership.class));
    }

    @Test
    void shouldAllowDemotingAnOwnerWhenAnotherActiveOwnerExists() {
        when(membershipRepository.countByOrganization_IdAndRoleAndActiveTrue(organization.getId(), OrganizationRole.OWNER)).thenReturn(2L);

        OrganizationMemberResponse response = service.changeRole(
                ownerUser.getId(),
                new UpdateOrganizationMemberRoleRequest(OrganizationRole.ADMIN)
        );

        assertEquals(OrganizationRole.ADMIN, response.role());
        assertEquals(OrganizationRole.ADMIN, ownerMembership.getRole());
        verify(membershipRepository).save(ownerMembership);
    }
}
