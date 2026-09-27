package com.linepulse.organization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, Long> {
    List<OrganizationMembership> findByUser_RegistrationIgnoreCaseAndActiveTrueOrderByOrganization_NameAsc(String registration);
    Optional<OrganizationMembership> findFirstByUser_RegistrationIgnoreCaseAndActiveTrueOrderByCreatedAtAsc(String registration);
    Optional<OrganizationMembership> findByOrganization_IdAndUser_RegistrationIgnoreCaseAndActiveTrue(UUID organizationId, String registration);
    List<OrganizationMembership> findByOrganization_IdAndActiveTrueOrderByUser_NameAsc(UUID organizationId);
    boolean existsByOrganization_IdAndUser_Id(UUID organizationId, UUID userId);
}
