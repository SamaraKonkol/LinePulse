package com.linepulse.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServiceRequestLifecycleTest {
    @Test
    void shouldTrackExternalServiceLifecycleAndCompanyApproval() {
        Instant requestedAt = Instant.parse("2026-09-27T10:00:00Z");
        ServiceRequest request = new ServiceRequest(
                UUID.randomUUID(), null, null, null, null,
                "Falha elétrica", "Máquina parada", ServiceRequestChannel.EXTERNAL,
                ServiceRequestPriority.CRITICAL, requestedAt
        );

        assertThat(request.getStatus()).isEqualTo(ServiceRequestStatus.REQUESTED);

        request.accept(Instant.parse("2026-09-27T11:00:00Z"), requestedAt.plusSeconds(60));
        assertThat(request.getStatus()).isEqualTo(ServiceRequestStatus.ACCEPTED);
        assertThat(request.getEta()).isEqualTo(Instant.parse("2026-09-27T11:00:00Z"));

        request.markEnRoute(requestedAt.plusSeconds(120));
        request.start(requestedAt.plusSeconds(180));
        request.complete("Substituição do contator", "Contator 24V", requestedAt.plusSeconds(3600));
        assertThat(request.getStatus()).isEqualTo(ServiceRequestStatus.COMPLETED);
        assertThat(request.getServiceNotes()).isEqualTo("Substituição do contator");

        request.approve(requestedAt.plusSeconds(3660));
        assertThat(request.getStatus()).isEqualTo(ServiceRequestStatus.APPROVED);
        assertThat(request.getApprovedAt()).isNotNull();
    }
}
