package com.linepulse.asset;

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
@RequestMapping("/api/machines")
public class MachineController {
    private final MachineService machineService;

    public MachineController(MachineService machineService) {
        this.machineService = machineService;
    }

    @GetMapping
    List<MachineResponse> findAll() {
        return machineService.findAll();
    }

    @PostMapping
    ResponseEntity<MachineResponse> create(@Valid @RequestBody CreateMachineRequest request) {
        MachineResponse created = machineService.create(request);
        return ResponseEntity.created(URI.create("/api/machines/" + created.id())).body(created);
    }
}
