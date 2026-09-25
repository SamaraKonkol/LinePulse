package com.linepulse.incident;

import com.linepulse.asset.Machine;
import com.linepulse.asset.MachineRepository;
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

    public IncidentService(IncidentRepository incidentRepository, MachineRepository machineRepository) {
        this.incidentRepository = incidentRepository;
        this.machineRepository = machineRepository;
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
        return IncidentResponse.from(incidentRepository.save(incident));
    }
}
