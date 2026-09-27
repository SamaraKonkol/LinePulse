package com.linepulse.maintenance;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenancePlanRepository extends JpaRepository<MaintenancePlan, UUID> {
    List<MaintenancePlan> findAllByOrderByNextDueDateAsc();
    List<MaintenancePlan> findByActiveTrueAndNextDueDateLessThanEqualOrderByNextDueDateAsc(LocalDate date);
    List<MaintenancePlan> findByActiveTrueAndNextDueDateLessThanEqual(LocalDate date);
}
