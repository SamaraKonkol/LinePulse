package com.linepulse.maintenance;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {
    List<WorkOrder> findAllByOrderByCreatedAtDesc();
    List<WorkOrder> findAllByMachine_ProductionLine_Sector_Plant_Organization_IdOrderByCreatedAtDesc(UUID organizationId);
    Optional<WorkOrder> findByIdAndMachine_ProductionLine_Sector_Plant_Organization_Id(UUID id, UUID organizationId);
    List<WorkOrder> findByStatusAndCompletedAtAfter(WorkOrderStatus status, Instant completedAfter);
    List<WorkOrder> findByMachine_ProductionLine_Sector_Plant_Organization_IdAndStatusAndCompletedAtAfter(UUID organizationId, WorkOrderStatus status, Instant completedAfter);
    long countByStatusIn(List<WorkOrderStatus> statuses);
    long countByMachine_ProductionLine_Sector_Plant_Organization_IdAndStatusIn(UUID organizationId, List<WorkOrderStatus> statuses);
}
