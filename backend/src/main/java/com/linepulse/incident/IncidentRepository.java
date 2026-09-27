package com.linepulse.incident;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    List<Incident> findAllByOrderByCreatedAtDesc();
    List<Incident> findAllByMachine_ProductionLine_Sector_Plant_Organization_IdOrderByCreatedAtDesc(UUID organizationId);
    Page<Incident> findAllByMachine_ProductionLine_Sector_Plant_Organization_Id(UUID organizationId, Pageable pageable);
    Optional<Incident> findByIdAndMachine_ProductionLine_Sector_Plant_Organization_Id(UUID id, UUID organizationId);
    List<Incident> findByOccurredAtAfterOrderByOccurredAtAsc(Instant occurredAfter);
    List<Incident> findByMachine_ProductionLine_Sector_Plant_Organization_IdAndOccurredAtAfterOrderByOccurredAtAsc(UUID organizationId, Instant occurredAfter);
    long countByStatusIn(List<IncidentStatus> statuses);
    long countByMachine_ProductionLine_Sector_Plant_Organization_IdAndStatusIn(UUID organizationId, List<IncidentStatus> statuses);
}
