package com.linepulse.incident;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    List<Incident> findAllByOrderByCreatedAtDesc();
    long countByStatusIn(List<IncidentStatus> statuses);
}
