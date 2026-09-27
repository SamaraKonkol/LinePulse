package com.linepulse.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProviderOrganizationRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 120) @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
        @NotBlank @Size(max = 120) String ownerName,
        @NotBlank @Size(max = 40) String ownerRegistration,
        @NotBlank @Size(min = 8, max = 72) String ownerPassword
) {
}
