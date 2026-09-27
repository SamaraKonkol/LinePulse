package com.linepulse.audit;

import com.linepulse.common.PageResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-events")
public class AuditController {
    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    List<AuditEventResponse> findRecent() {
        return auditService.findRecent();
    }

    @GetMapping("/page")
    PageResponse<AuditEventResponse> findPage(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return auditService.findPage(page, size);
    }
}
