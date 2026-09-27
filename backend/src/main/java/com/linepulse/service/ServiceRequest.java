package com.linepulse.service;

import com.linepulse.asset.Machine;
import com.linepulse.auth.UserAccount;
import com.linepulse.incident.Incident;
import com.linepulse.organization.Organization;
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
@Table(name = "service_requests")
public class ServiceRequest {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_organization_id")
    private Organization company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_organization_id")
    private Organization provider;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id")
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_technician_id")
    private UserAccount assignedTechnician;

    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    private ServiceRequestChannel channel;

    @Enumerated(EnumType.STRING)
    private ServiceRequestPriority priority;

    @Enumerated(EnumType.STRING)
    private ServiceRequestStatus status;

    private Instant eta;
    private String serviceNotes;
    private String partsUsed;
    private String declineReason;
    private Instant requestedAt;
    private Instant acceptedAt;
    private Instant enRouteAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant approvedAt;
    private Instant declinedAt;
    private Instant cancelledAt;
    private Instant updatedAt;

    protected ServiceRequest() {
    }

    public ServiceRequest(UUID id, Organization company, Organization provider, Machine machine, Incident incident, String title, String description, ServiceRequestChannel channel, ServiceRequestPriority priority, Instant requestedAt) {
        this.id = id;
        this.company = company;
        this.provider = provider;
        this.machine = machine;
        this.incident = incident;
        this.title = title;
        this.description = description;
        this.channel = channel;
        this.priority = priority;
        this.status = ServiceRequestStatus.REQUESTED;
        this.requestedAt = requestedAt;
        this.updatedAt = requestedAt;
    }

    public void assignTechnician(UserAccount technician, Instant at) {
        this.assignedTechnician = technician;
        this.updatedAt = at;
    }

    public void accept(Instant eta, Instant at) {
        this.status = ServiceRequestStatus.ACCEPTED;
        this.eta = eta;
        this.acceptedAt = at;
        this.updatedAt = at;
    }

    public void updateEta(Instant eta, Instant at) {
        this.eta = eta;
        this.updatedAt = at;
    }

    public void markEnRoute(Instant at) {
        this.status = ServiceRequestStatus.EN_ROUTE;
        this.enRouteAt = at;
        this.updatedAt = at;
    }

    public void start(Instant at) {
        this.status = ServiceRequestStatus.IN_PROGRESS;
        this.startedAt = at;
        this.updatedAt = at;
    }

    public void complete(String serviceNotes, String partsUsed, Instant at) {
        this.status = ServiceRequestStatus.COMPLETED;
        this.serviceNotes = serviceNotes;
        this.partsUsed = partsUsed;
        this.completedAt = at;
        this.updatedAt = at;
    }

    public void approve(Instant at) {
        this.status = ServiceRequestStatus.APPROVED;
        this.approvedAt = at;
        this.updatedAt = at;
    }

    public void decline(String reason, Instant at) {
        this.status = ServiceRequestStatus.DECLINED;
        this.declineReason = reason;
        this.declinedAt = at;
        this.updatedAt = at;
    }

    public void cancel(Instant at) {
        this.status = ServiceRequestStatus.CANCELLED;
        this.cancelledAt = at;
        this.updatedAt = at;
    }

    public UUID getId() { return id; }
    public Organization getCompany() { return company; }
    public Organization getProvider() { return provider; }
    public Machine getMachine() { return machine; }
    public Incident getIncident() { return incident; }
    public UserAccount getAssignedTechnician() { return assignedTechnician; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public ServiceRequestChannel getChannel() { return channel; }
    public ServiceRequestPriority getPriority() { return priority; }
    public ServiceRequestStatus getStatus() { return status; }
    public Instant getEta() { return eta; }
    public String getServiceNotes() { return serviceNotes; }
    public String getPartsUsed() { return partsUsed; }
    public String getDeclineReason() { return declineReason; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getEnRouteAt() { return enRouteAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getApprovedAt() { return approvedAt; }
    public Instant getDeclinedAt() { return declinedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
}
