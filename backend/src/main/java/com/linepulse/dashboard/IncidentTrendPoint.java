package com.linepulse.dashboard;

import java.time.LocalDate;

public record IncidentTrendPoint(
        LocalDate date,
        long incidents
) {
}
