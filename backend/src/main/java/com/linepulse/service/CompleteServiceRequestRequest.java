package com.linepulse.service;

import jakarta.validation.constraints.NotBlank;

public record CompleteServiceRequestRequest(@NotBlank String serviceNotes, String partsUsed) {
}
