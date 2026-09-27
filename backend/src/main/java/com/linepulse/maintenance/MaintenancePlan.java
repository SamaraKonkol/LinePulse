package com.linepulse.maintenance;

import com.linepulse.asset.Machine;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "maintenance_plans")
public class MaintenancePlan {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id")
    private Machine machine;

    private String title;
    private String description;
    private int intervalDays;
    private LocalDate nextDueDate;

    @Enumerated(EnumType.STRING)
    private WorkOrderPriority priority;

    private boolean active;
    private Instant lastGeneratedAt;
    private Instant createdAt;
    private Instant updatedAt;

    protected MaintenancePlan() {
    }

    public MaintenancePlan(UUID id, Machine machine, String title, String description, int intervalDays, LocalDate nextDueDate, WorkOrderPriority priority, boolean active, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.machine = machine;
        this.title = title;
        this.description = description;
        this.intervalDays = intervalDays;
        this.nextDueDate = nextDueDate;
        this.priority = priority;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void update(Machine machine, String title, String description, int intervalDays, LocalDate nextDueDate, WorkOrderPriority priority, Instant at) {
        this.machine = machine;
        this.title = title;
        this.description = description;
        this.intervalDays = intervalDays;
        this.nextDueDate = nextDueDate;
        this.priority = priority;
        this.updatedAt = at;
    }

    public void changeActive(boolean active, Instant at) {
        this.active = active;
        this.updatedAt = at;
    }

    public void markGenerated(Instant at, LocalDate referenceDate) {
        this.lastGeneratedAt = at;
        do {
            this.nextDueDate = this.nextDueDate.plusDays(intervalDays);
        } while (!this.nextDueDate.isAfter(referenceDate));
        this.updatedAt = at;
    }

    public UUID getId() { return id; }
    public Machine getMachine() { return machine; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getIntervalDays() { return intervalDays; }
    public LocalDate getNextDueDate() { return nextDueDate; }
    public WorkOrderPriority getPriority() { return priority; }
    public boolean isActive() { return active; }
    public Instant getLastGeneratedAt() { return lastGeneratedAt; }
}
