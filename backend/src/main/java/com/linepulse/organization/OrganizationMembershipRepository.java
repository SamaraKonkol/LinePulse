package com.linepulse.organization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, Long> {
    @EntityGraph(attributePaths = {"organization", "user"})
    List<OrganizationMembership> findByUser_RegistrationIgnoreCaseAndActiveTrueOrderByOrganization_NameAsc(String registration);

    @EntityGraph(attributePaths = {"organization", "user"})
    Optional<OrganizationMembership> findFirstByUser_RegistrationIgnoreCaseAndActiveTrueOrderByCreatedAtAsc(String registration);

    @EntityGraph(attributePaths = {"organization", "user"})
    Optional<OrganizationMembership> findByOrganization_IdAndUser_RegistrationIgnoreCaseAndActiveTrue(UUID organizationId, String registration);

    @EntityGraph(attributePaths = {"organization", "user"})
    Optional<OrganizationMembership> findByOrganization_IdAndUser_Id(UUID organizationId, UUID userId);

    @EntityGraph(attributePaths = {"organization", "user"})
    List<OrganizationMembership> findByOrganization_IdOrderByUser_NameAsc(UUID organizationId);

    @EntityGraph(attributePaths = {"organization", "user"})
    List<OrganizationMembership> findByOrganization_IdAndActiveTrueOrderByUser_NameAsc(UUID organizationId);

    long countByOrganization_IdAndRoleAndActiveTrue(UUID organizationId, OrganizationRole role);

    boolean existsByOrganization_IdAndUser_Id(UUID organizationId, UUID userId);
    boolean existsByUser_RegistrationIgnoreCase(String registration);
}
