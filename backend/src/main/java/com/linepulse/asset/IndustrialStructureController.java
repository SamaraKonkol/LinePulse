package com.linepulse.asset;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/structure")
public class IndustrialStructureController {
    private final IndustrialStructureService service;

    public IndustrialStructureController(IndustrialStructureService service) {
        this.service = service;
    }

    @GetMapping("/plants")
    List<PlantResponse> plants() {
        return service.findPlants();
    }

    @PostMapping("/plants")
    ResponseEntity<PlantResponse> createPlant(@Valid @RequestBody CreatePlantRequest request) {
        PlantResponse created = service.createPlant(request);
        return ResponseEntity.created(URI.create("/api/admin/structure/plants/" + created.id())).body(created);
    }

    @PatchMapping("/plants/{id}")
    PlantResponse updatePlant(@PathVariable UUID id, @Valid @RequestBody UpdatePlantRequest request) {
        return service.updatePlant(id, request);
    }

    @PatchMapping("/plants/{id}/status")
    PlantResponse updatePlantStatus(@PathVariable UUID id, @RequestBody UpdateStructureStatusRequest request) {
        return service.updatePlantStatus(id, request);
    }

    @GetMapping("/sectors")
    List<SectorResponse> sectors() {
        return service.findSectors();
    }

    @PostMapping("/sectors")
    ResponseEntity<SectorResponse> createSector(@Valid @RequestBody CreateSectorRequest request) {
        SectorResponse created = service.createSector(request);
        return ResponseEntity.created(URI.create("/api/admin/structure/sectors/" + created.id())).body(created);
    }

    @PatchMapping("/sectors/{id}")
    SectorResponse updateSector(@PathVariable UUID id, @Valid @RequestBody UpdateSectorRequest request) {
        return service.updateSector(id, request);
    }

    @PatchMapping("/sectors/{id}/status")
    SectorResponse updateSectorStatus(@PathVariable UUID id, @RequestBody UpdateStructureStatusRequest request) {
        return service.updateSectorStatus(id, request);
    }

    @GetMapping("/lines")
    List<ProductionLineResponse> lines() {
        return service.findLines();
    }

    @PostMapping("/lines")
    ResponseEntity<ProductionLineResponse> createLine(@Valid @RequestBody CreateProductionLineRequest request) {
        ProductionLineResponse created = service.createLine(request);
        return ResponseEntity.created(URI.create("/api/admin/structure/lines/" + created.id())).body(created);
    }

    @PatchMapping("/lines/{id}")
    ProductionLineResponse updateLine(@PathVariable UUID id, @Valid @RequestBody UpdateProductionLineRequest request) {
        return service.updateLine(id, request);
    }

    @PatchMapping("/lines/{id}/status")
    ProductionLineResponse updateLineStatus(@PathVariable UUID id, @RequestBody UpdateStructureStatusRequest request) {
        return service.updateLineStatus(id, request);
    }
}
