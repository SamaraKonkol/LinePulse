package com.linepulse.incident;

import com.linepulse.asset.Machine;
import com.linepulse.asset.MachineRepository;
import com.linepulse.audit.AuditService;
import com.linepulse.common.ConflictException;
import com.linepulse.common.NotFoundException;
import com.linepulse.common.PageResponse;
import com.linepulse.organization.OrganizationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentService {
    private final IncidentRepository incidentRepository;
    private final MachineRepository machineRepository;
    private final AuditService auditService;
    private final OrganizationService organizationService;

    public IncidentService(IncidentRepository incidentRepository, MachineRepository machineRepository, AuditService auditService, OrganizationService organizationService) {
        this.incidentRepository = incidentRepository;
        this.machineRepository = machineRepository;
        this.auditService = auditService;
        this.organizationService = organizationService;
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> findAll() {
        UUID organizationId = organizationService.currentOrganization().getId();
        return incidentRepository.findAllByMachine_ProductionLine_Sector_Plant_Organization_IdOrderByCreatedAtDesc(organizationId).stream()
                .map(IncidentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<IncidentResponse> findPage(int page, int size) {
        UUID organizationId = organizationService.currentOrganization().getId();
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<Incident> result = incidentRepository.findAllByMachine_ProductionLine_Sector_Plant_Organization_Id(
                organizationId,
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResponse.from(result, result.getContent().stream().map(IncidentResponse::from).toList());
    }

    @Transactional
    public IncidentResponse create(CreateIncidentRequest request) {
        UUID organizationId = organizationService.currentOrganization().getId();
        Machine machine = machineRepository.findByIdAndProductionLine_Sector_Plant_Organization_Id(request.machineId(), organizationId)
                .orElseThrow(() -> new NotFoundException("Machine not found"));
        Instant now = Instant.now();
        Incident incident = new Incident(
                UUID.randomUUID(),
                machine,
                request.title().trim(),
                request.description().trim(),
                request.category(),
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
    public IncidentResponse resolve(UUID id, ResolveIncidentRequest request) {
        Incident incident = findIncident(id);
        if (!IncidentLifecycle.canResolve(incident.getStatus())) {
            throw new ConflictException("Only incidents in progress can be resolved");
        }
        incident.resolve(request.rootCause().trim(), request.solution().trim(), Instant.now());
        Incident saved = incidentRepository.save(incident);
        auditService.record("INCIDENT_RESOLVED", "INCIDENT", saved.getId(), "Ocorrência resolvida em " + saved.getMachine().getAssetCode() + " · causa: " + saved.getRootCause());
        return IncidentResponse.from(saved);
    }

    @Transactional
    public IncidentResponse resolve(UUID id) {
        return resolve(id, new ResolveIncidentRequest("Não informado", "Não informado"));
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
        UUID organizationId = organizationService.currentOrganization().getId();
        return incidentRepository.findByIdAndMachine_ProductionLine_Sector_Plant_Organization_Id(id, organizationId)
                .orElseThrow(() -> new NotFoundException("Incident not found"));
    }
}
