package com.linepulse.alert;

import java.time.Instant;
import java.util.UUID;

public record OperationalAlert(
        String key,
        AlertSeverity severity,
        String title,
        String message,
        String sourceType,
        UUID sourceId,
        Instant detectedAt
) {
}
