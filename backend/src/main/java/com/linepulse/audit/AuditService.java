package com.linepulse.audit;

import com.linepulse.common.PageResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public void record(String action, String entityType, UUID entityId, String description) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String actorRegistration = authentication != null && authentication.isAuthenticated()
                ? authentication.getName()
                : "SYSTEM";
        AuditEvent event = new AuditEvent(
                UUID.randomUUID(), action, entityType, entityId, description, actorRegistration, Instant.now()
        );
        auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> findRecent() {
        return auditEventRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(AuditEventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventResponse> findPage(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<AuditEvent> result = auditEventRepository.findAll(PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(result, result.getContent().stream().map(AuditEventResponse::from).toList());
    }
}
