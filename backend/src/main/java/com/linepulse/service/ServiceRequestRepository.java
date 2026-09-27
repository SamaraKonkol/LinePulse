package com.linepulse.service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {
    Page<ServiceRequest> findByCompany_Id(UUID companyId, Pageable pageable);
    Page<ServiceRequest> findByProvider_Id(UUID providerId, Pageable pageable);
    Page<ServiceRequest> findByCompany_IdAndStatus(UUID companyId, ServiceRequestStatus status, Pageable pageable);
    Page<ServiceRequest> findByProvider_IdAndStatus(UUID providerId, ServiceRequestStatus status, Pageable pageable);
    Optional<ServiceRequest> findByIdAndCompany_Id(UUID id, UUID companyId);
    Optional<ServiceRequest> findByIdAndProvider_Id(UUID id, UUID providerId);
}
