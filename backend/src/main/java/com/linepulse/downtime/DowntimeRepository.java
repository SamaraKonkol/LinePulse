package com.linepulse.downtime;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DowntimeRepository extends JpaRepository<Downtime, UUID> {
    List<Downtime> findAllByOrderByStartedAtDesc();
    List<Downtime> findAllByMachine_ProductionLine_Sector_Plant_Organization_IdOrderByStartedAtDesc(UUID organizationId);
    Optional<Downtime> findByIdAndMachine_ProductionLine_Sector_Plant_Organization_Id(UUID id, UUID organizationId);

    @Query("select d from Downtime d where d.startedAt < :end and (d.endedAt is null or d.endedAt > :start)")
    List<Downtime> findOverlapping(@Param("start") Instant start, @Param("end") Instant end);

    @Query("select d from Downtime d where d.machine.productionLine.sector.plant.organization.id = :organizationId and d.startedAt < :end and (d.endedAt is null or d.endedAt > :start)")
    List<Downtime> findOverlappingByOrganization(@Param("organizationId") UUID organizationId, @Param("start") Instant start, @Param("end") Instant end);
}
