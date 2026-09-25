package com.linepulse.incident;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {
    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    List<IncidentResponse> findAll() {
        return incidentService.findAll();
    }

    @PostMapping
    ResponseEntity<IncidentResponse> create(@Valid @RequestBody CreateIncidentRequest request) {
        IncidentResponse created = incidentService.create(request);
        return ResponseEntity.created(URI.create("/api/incidents/" + created.id())).body(created);
    }
}
