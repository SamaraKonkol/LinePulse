package com.linepulse.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRelationshipRepository extends JpaRepository<ServiceRelationship, UUID> {
    List<ServiceRelationship> findByCompany_IdOrderByCreatedAtDesc(UUID companyId);
    Optional<ServiceRelationship> findByIdAndCompany_Id(UUID id, UUID companyId);
    Optional<ServiceRelationship> findByCompany_IdAndProvider_Id(UUID companyId, UUID providerId);
    boolean existsByCompany_IdAndProvider_IdAndStatus(UUID companyId, UUID providerId, ServiceRelationshipStatus status);
}
