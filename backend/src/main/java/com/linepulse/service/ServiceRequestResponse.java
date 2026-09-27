package com.linepulse.service;

import java.time.Instant;
import java.util.UUID;

public record ServiceRequestResponse(
        UUID id,
        UUID companyOrganizationId,
        String companyName,
        UUID providerOrganizationId,
        String providerName,
        UUID machineId,
        String machineName,
        String machineAssetCode,
        UUID incidentId,
        String incidentTitle,
        UUID assignedTechnicianId,
        String assignedTechnicianName,
        String title,
        String description,
        ServiceRequestChannel channel,
        ServiceRequestPriority priority,
        ServiceRequestStatus status,
        Instant eta,
        String serviceNotes,
        String partsUsed,
        String declineReason,
        Instant requestedAt,
        Instant acceptedAt,
        Instant enRouteAt,
        Instant startedAt,
        Instant completedAt,
        Instant approvedAt,
        Instant declinedAt,
        Instant cancelledAt
) {
    public static ServiceRequestResponse from(ServiceRequest request) {
        return new ServiceRequestResponse(
                request.getId(),
                request.getCompany().getId(),
                request.getCompany().getName(),
                request.getProvider() == null ? null : request.getProvider().getId(),
                request.getProvider() == null ? null : request.getProvider().getName(),
                request.getMachine().getId(),
                request.getMachine().getName(),
                request.getMachine().getAssetCode(),
                request.getIncident() == null ? null : request.getIncident().getId(),
                request.getIncident() == null ? null : request.getIncident().getTitle(),
                request.getAssignedTechnician() == null ? null : request.getAssignedTechnician().getId(),
                request.getAssignedTechnician() == null ? null : request.getAssignedTechnician().getName(),
                request.getTitle(),
                request.getDescription(),
                request.getChannel(),
                request.getPriority(),
                request.getStatus(),
                request.getEta(),
                request.getServiceNotes(),
                request.getPartsUsed(),
                request.getDeclineReason(),
                request.getRequestedAt(),
                request.getAcceptedAt(),
                request.getEnRouteAt(),
                request.getStartedAt(),
                request.getCompletedAt(),
                request.getApprovedAt(),
                request.getDeclinedAt(),
                request.getCancelledAt()
        );
    }
}
