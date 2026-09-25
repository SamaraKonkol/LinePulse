package com.linepulse.downtime;

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
@RequestMapping("/api/downtimes")
public class DowntimeController {
    private final DowntimeService downtimeService;

    public DowntimeController(DowntimeService downtimeService) {
        this.downtimeService = downtimeService;
    }

    @GetMapping
    List<DowntimeResponse> findAll() {
        return downtimeService.findAll();
    }

    @PostMapping
    ResponseEntity<DowntimeResponse> create(@Valid @RequestBody CreateDowntimeRequest request) {
        DowntimeResponse created = downtimeService.create(request);
        return ResponseEntity.created(URI.create("/api/downtimes/" + created.id())).body(created);
    }

    @PatchMapping("/{id}/close")
    DowntimeResponse close(@PathVariable UUID id, @RequestBody CloseDowntimeRequest request) {
        return downtimeService.close(id, request);
    }
}
