package com.linepulse.service;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record UpdateServiceRequestEtaRequest(@NotNull Instant eta) {
}
