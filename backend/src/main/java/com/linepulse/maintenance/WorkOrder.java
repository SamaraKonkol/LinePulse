package com.linepulse.maintenance;

import com.linepulse.asset.Machine;
import com.linepulse.incident.Incident;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "work_orders")
public class WorkOrder {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id")
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    private MaintenanceType type;

    @Enumerated(EnumType.STRING)
    private WorkOrderPriority priority;

    @Enumerated(EnumType.STRING)
    private WorkOrderStatus status;

    private Instant scheduledFor;
    private Instant startedAt;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;

    protected WorkOrder() {
    }

    public WorkOrder(UUID id, Machine machine, Incident incident, String title, String description, MaintenanceType type, WorkOrderPriority priority, WorkOrderStatus status, Instant scheduledFor, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.machine = machine;
        this.incident = incident;
        this.title = title;
        this.description = description;
        this.type = type;
        this.priority = priority;
        this.status = status;
        this.scheduledFor = scheduledFor;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void start(Instant startedAt) {
        this.status = WorkOrderStatus.IN_PROGRESS;
        this.startedAt = startedAt;
        this.updatedAt = startedAt;
    }

    public void complete(Instant completedAt) {
        this.status = WorkOrderStatus.COMPLETED;
        this.completedAt = completedAt;
        this.updatedAt = completedAt;
    }

    public UUID getId() { return id; }
    public Machine getMachine() { return machine; }
    public Incident getIncident() { return incident; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public MaintenanceType getType() { return type; }
    public WorkOrderPriority getPriority() { return priority; }
    public WorkOrderStatus getStatus() { return status; }
    public Instant getScheduledFor() { return scheduledFor; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
