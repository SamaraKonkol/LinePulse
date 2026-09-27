package com.linepulse.organization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    Optional<Organization> findBySlugIgnoreCase(String slug);
    List<Organization> findByTypeAndActiveTrueOrderByNameAsc(OrganizationType type);
}
