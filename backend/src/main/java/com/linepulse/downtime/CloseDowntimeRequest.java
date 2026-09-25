package com.linepulse.downtime;

import java.time.Instant;

public record CloseDowntimeRequest(Instant endedAt) {
}
