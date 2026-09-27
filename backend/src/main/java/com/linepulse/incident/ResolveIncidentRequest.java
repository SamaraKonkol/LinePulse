package com.linepulse.incident;

import jakarta.validation.constraints.NotBlank;

public record ResolveIncidentRequest(
        @NotBlank String rootCause,
        @NotBlank String solution
) {
}
