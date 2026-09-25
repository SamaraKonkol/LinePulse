package com.linepulse.downtime;

import com.linepulse.asset.Machine;
import com.linepulse.asset.MachineRepository;
import com.linepulse.audit.AuditService;
import com.linepulse.common.ConflictException;
import com.linepulse.common.NotFoundException;
import com.linepulse.incident.Incident;
import com.linepulse.incident.IncidentRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DowntimeService {
    private final DowntimeRepository downtimeRepository;
    private final MachineRepository machineRepository;
    private final IncidentRepository incidentRepository;
    private final AuditService auditService;

    public DowntimeService(DowntimeRepository downtimeRepository, MachineRepository machineRepository, IncidentRepository incidentRepository, AuditService auditService) {
        this.downtimeRepository = downtimeRepository;
        this.machineRepository = machineRepository;
        this.incidentRepository = incidentRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<DowntimeResponse> findAll() {
        return downtimeRepository.findAllByOrderByStartedAtDesc().stream()
                .map(DowntimeResponse::from)
                .toList();
    }

    @Transactional
    public DowntimeResponse create(CreateDowntimeRequest request) {
        Machine machine = machineRepository.findById(request.machineId())
                .orElseThrow(() -> new NotFoundException("Machine not found"));
        Incident incident = resolveIncident(request.incidentId(), machine);
        Instant now = Instant.now();
        Downtime downtime = new Downtime(
                UUID.randomUUID(),
                machine,
                incident,
                request.reason().trim(),
                request.startedAt() == null ? now : request.startedAt(),
                now
        );
        Downtime saved = downtimeRepository.save(downtime);
        auditService.record("DOWNTIME_STARTED", "DOWNTIME", saved.getId(), "Parada registrada em " + machine.getAssetCode());
        return DowntimeResponse.from(saved);
    }

    @Transactional
    public DowntimeResponse close(UUID id, CloseDowntimeRequest request) {
        Downtime downtime = downtimeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Downtime not found"));
        if (downtime.getEndedAt() != null) {
            throw new ConflictException("Downtime is already closed");
        }
        Instant endedAt = request.endedAt() == null ? Instant.now() : request.endedAt();
        if (!endedAt.isAfter(downtime.getStartedAt())) {
            throw new IllegalArgumentException("Downtime end must be after start");
        }
        downtime.close(endedAt);
        Downtime saved = downtimeRepository.save(downtime);
        auditService.record("DOWNTIME_CLOSED", "DOWNTIME", saved.getId(), "Parada encerrada em " + saved.getMachine().getAssetCode());
        return DowntimeResponse.from(saved);
    }

    private Incident resolveIncident(UUID incidentId, Machine machine) {
        if (incidentId == null) {
            return null;
        }
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new NotFoundException("Incident not found"));
        if (!incident.getMachine().getId().equals(machine.getId())) {
            throw new IllegalArgumentException("Incident does not belong to selected machine");
        }
        return incident;
    }
}
