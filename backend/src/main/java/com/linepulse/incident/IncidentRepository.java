package com.linepulse.incident;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    List<Incident> findAllByOrderByCreatedAtDesc();
    List<Incident> findByOccurredAtAfterOrderByOccurredAtAsc(Instant occurredAfter);
    long countByStatusIn(List<IncidentStatus> statuses);
}
