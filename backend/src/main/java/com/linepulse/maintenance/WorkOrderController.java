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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {
    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @GetMapping
    List<WorkOrderResponse> findAll() {
        return workOrderService.findAll();
    }

    @PostMapping
    ResponseEntity<WorkOrderResponse> create(@Valid @RequestBody CreateWorkOrderRequest request) {
        WorkOrderResponse created = workOrderService.create(request);
        return ResponseEntity.created(URI.create("/api/work-orders/" + created.id())).body(created);
    }

    @PatchMapping("/{id}/start")
    WorkOrderResponse start(@PathVariable UUID id) {
        return workOrderService.start(id);
    }

    @PatchMapping("/{id}/complete")
    WorkOrderResponse complete(@PathVariable UUID id) {
        return workOrderService.complete(id);
    }
}
