package com.linepulse.service;

import com.linepulse.common.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/service-requests")
public class ServiceRequestController {
    private final ServiceRequestService serviceRequestService;

    public ServiceRequestController(ServiceRequestService serviceRequestService) {
        this.serviceRequestService = serviceRequestService;
    }

    @GetMapping
    PageResponse<ServiceRequestResponse> list(
            @RequestParam(required = false) ServiceRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return serviceRequestService.list(status, page, size);
    }

    @GetMapping("/{id}")
    ServiceRequestResponse get(@PathVariable UUID id) {
        return serviceRequestService.get(id);
    }

    @PostMapping
    ServiceRequestResponse create(@Valid @RequestBody CreateServiceRequestRequest request) {
        return serviceRequestService.create(request);
    }

    @PatchMapping("/{id}/accept")
    ServiceRequestResponse accept(@PathVariable UUID id, @RequestBody AcceptServiceRequestRequest request) {
        return serviceRequestService.accept(id, request);
    }

    @PatchMapping("/{id}/decline")
    ServiceRequestResponse decline(@PathVariable UUID id, @Valid @RequestBody DeclineServiceRequestRequest request) {
        return serviceRequestService.decline(id, request);
    }

    @PatchMapping("/{id}/assign")
    ServiceRequestResponse assign(@PathVariable UUID id, @Valid @RequestBody AssignServiceRequestRequest request) {
        return serviceRequestService.assign(id, request);
    }

    @PatchMapping("/{id}/eta")
    ServiceRequestResponse updateEta(@PathVariable UUID id, @Valid @RequestBody UpdateServiceRequestEtaRequest request) {
        return serviceRequestService.updateEta(id, request);
    }

    @PatchMapping("/{id}/en-route")
    ServiceRequestResponse markEnRoute(@PathVariable UUID id) {
        return serviceRequestService.markEnRoute(id);
    }

    @PatchMapping("/{id}/start")
    ServiceRequestResponse start(@PathVariable UUID id) {
        return serviceRequestService.start(id);
    }

    @PatchMapping("/{id}/complete")
    ServiceRequestResponse complete(@PathVariable UUID id, @Valid @RequestBody CompleteServiceRequestRequest request) {
        return serviceRequestService.complete(id, request);
    }

    @PatchMapping("/{id}/approve")
    ServiceRequestResponse approve(@PathVariable UUID id) {
        return serviceRequestService.approve(id);
    }

    @PatchMapping("/{id}/cancel")
    ServiceRequestResponse cancel(@PathVariable UUID id) {
        return serviceRequestService.cancel(id);
    }
}
