package com.linepulse.downtime;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DowntimeRepository extends JpaRepository<Downtime, UUID> {
    List<Downtime> findAllByOrderByStartedAtDesc();

    @Query("select d from Downtime d where d.startedAt < :end and (d.endedAt is null or d.endedAt > :start)")
    List<Downtime> findOverlapping(@Param("start") Instant start, @Param("end") Instant end);
}
