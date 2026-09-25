package com.linepulse.incident;

import com.linepulse.asset.Machine;
import com.linepulse.asset.MachineRepository;
import com.linepulse.audit.AuditService;
import com.linepulse.common.ConflictException;
import com.linepulse.common.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentService {
    private final IncidentRepository incidentRepository;
    private final MachineRepository machineRepository;
    private final AuditService auditService;

    public IncidentService(IncidentRepository incidentRepository, MachineRepository machineRepository, AuditService auditService) {
        this.incidentRepository = incidentRepository;
        this.machineRepository = machineRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> findAll() {
        return incidentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(IncidentResponse::from)
                .toList();
    }

    @Transactional
    public IncidentResponse create(CreateIncidentRequest request) {
        Machine machine = machineRepository.findById(request.machineId())
                .orElseThrow(() -> new NotFoundException("Machine not found"));
        Instant now = Instant.now();
        Incident incident = new Incident(
                UUID.randomUUID(),
                machine,
                request.title().trim(),
                request.description().trim(),
                request.priority(),
                IncidentStatus.OPEN,
                request.occurredAt() == null ? now : request.occurredAt(),
                now,
                now
        );
        Incident saved = incidentRepository.save(incident);
        auditService.record("INCIDENT_CREATED", "INCIDENT", saved.getId(), "Ocorrência aberta em " + machine.getAssetCode() + ": " + saved.getTitle());
        return IncidentResponse.from(saved);
    }

    @Transactional
    public IncidentResponse start(UUID id) {
        Incident incident = findIncident(id);
        if (!IncidentLifecycle.canStart(incident.getStatus())) {
            throw new ConflictException("Only open incidents can be started");
        }
        incident.start(Instant.now());
        Incident saved = incidentRepository.save(incident);
        auditService.record("INCIDENT_STARTED", "INCIDENT", saved.getId(), "Ocorrência iniciada em " + saved.getMachine().getAssetCode());
        return IncidentResponse.from(saved);
    }

    @Transactional
    public IncidentResponse resolve(UUID id) {
        Incident incident = findIncident(id);
        if (!IncidentLifecycle.canResolve(incident.getStatus())) {
            throw new ConflictException("Only incidents in progress can be resolved");
        }
        incident.resolve(Instant.now());
        Incident saved = incidentRepository.save(incident);
        auditService.record("INCIDENT_RESOLVED", "INCIDENT", saved.getId(), "Ocorrência resolvida em " + saved.getMachine().getAssetCode());
        return IncidentResponse.from(saved);
    }

    @Transactional
    public IncidentResponse cancel(UUID id) {
        Incident incident = findIncident(id);
        if (!IncidentLifecycle.canCancel(incident.getStatus())) {
            throw new ConflictException("Only active incidents can be cancelled");
        }
        incident.cancel(Instant.now());
        Incident saved = incidentRepository.save(incident);
        auditService.record("INCIDENT_CANCELLED", "INCIDENT", saved.getId(), "Ocorrência cancelada em " + saved.getMachine().getAssetCode());
        return IncidentResponse.from(saved);
    }

    private Incident findIncident(UUID id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Incident not found"));
    }
}
