package com.linepulse.maintenance;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {
    List<WorkOrder> findAllByOrderByCreatedAtDesc();
    List<WorkOrder> findByStatusAndCompletedAtAfter(WorkOrderStatus status, Instant completedAfter);
    long countByStatusIn(List<WorkOrderStatus> statuses);
}
