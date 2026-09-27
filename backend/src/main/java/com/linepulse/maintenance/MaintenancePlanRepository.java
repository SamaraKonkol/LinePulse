package com.linepulse.maintenance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenancePlanRepository extends JpaRepository<MaintenancePlan, UUID> {
    List<MaintenancePlan> findAllByOrderByNextDueDateAsc();
    List<MaintenancePlan> findAllByMachine_ProductionLine_Sector_Plant_Organization_IdOrderByNextDueDateAsc(UUID organizationId);
    Optional<MaintenancePlan> findByIdAndMachine_ProductionLine_Sector_Plant_Organization_Id(UUID id, UUID organizationId);
    List<MaintenancePlan> findByActiveTrueAndNextDueDateLessThanEqualOrderByNextDueDateAsc(LocalDate date);
    List<MaintenancePlan> findByMachine_ProductionLine_Sector_Plant_Organization_IdAndActiveTrueAndNextDueDateLessThanEqualOrderByNextDueDateAsc(UUID organizationId, LocalDate date);
    List<MaintenancePlan> findByActiveTrueAndNextDueDateLessThanEqual(LocalDate date);
}
