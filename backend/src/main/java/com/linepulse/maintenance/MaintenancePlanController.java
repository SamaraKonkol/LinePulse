package com.linepulse.maintenance;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/maintenance-plans")
public class MaintenancePlanController {
    private final MaintenancePlanService maintenancePlanService;

    public MaintenancePlanController(MaintenancePlanService maintenancePlanService) {
        this.maintenancePlanService = maintenancePlanService;
    }

    @GetMapping
    List<MaintenancePlanResponse> findAll() {
        return maintenancePlanService.findAll();
    }

    @PostMapping
    ResponseEntity<MaintenancePlanResponse> create(@Valid @RequestBody MaintenancePlanRequest request) {
        MaintenancePlanResponse created = maintenancePlanService.create(request);
        return ResponseEntity.created(URI.create("/api/maintenance-plans/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    MaintenancePlanResponse update(@PathVariable UUID id, @Valid @RequestBody MaintenancePlanRequest request) {
        return maintenancePlanService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    MaintenancePlanResponse changeStatus(@PathVariable UUID id, @RequestBody MaintenancePlanStatusRequest request) {
        return maintenancePlanService.changeStatus(id, request.active());
    }

    @PostMapping("/{id}/generate")
    MaintenancePlanResponse generate(@PathVariable UUID id) {
        return maintenancePlanService.generate(id);
    }
}
