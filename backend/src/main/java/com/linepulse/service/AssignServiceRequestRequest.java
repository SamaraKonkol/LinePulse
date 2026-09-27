package com.linepulse.service;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignServiceRequestRequest(@NotNull UUID technicianId) {
}
