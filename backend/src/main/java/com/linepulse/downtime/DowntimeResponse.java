package com.linepulse.downtime;

import java.time.Instant;
import java.util.UUID;

public record DowntimeResponse(
        UUID id,
        UUID machineId,
        String assetCode,
        String machineName,
        UUID incidentId,
        String reason,
        Instant startedAt,
        Instant endedAt,
        Instant createdAt
) {
    static DowntimeResponse from(Downtime downtime) {
        return new DowntimeResponse(
                downtime.getId(),
                downtime.getMachine().getId(),
                downtime.getMachine().getAssetCode(),
                downtime.getMachine().getName(),
                downtime.getIncident() == null ? null : downtime.getIncident().getId(),
                downtime.getReason(),
                downtime.getStartedAt(),
                downtime.getEndedAt(),
                downtime.getCreatedAt()
        );
    }
}
