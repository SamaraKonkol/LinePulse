package com.linepulse.audit;

import com.linepulse.common.PageResponse;
import com.linepulse.organization.Organization;
import com.linepulse.organization.OrganizationService;
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
    @org.springframework.beans.factory.annotation.Autowired
    private com.linepulse.platform.PlatformAccess platformAccess;
    private final AuditEventRepository auditEventRepository;
    private final OrganizationService organizationService;

    public AuditService(AuditEventRepository auditEventRepository, OrganizationService organizationService) {
        this.auditEventRepository = auditEventRepository;
        this.organizationService = organizationService;
    }

    @Transactional
    public void record(String action, String entityType, UUID entityId, String description) {
        recordForOrganization(organizationService.currentOrganization(), action, entityType, entityId, description);
    }

    @Transactional
    public void recordForOrganization(Organization organization, String action, String entityType, UUID entityId, String description) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String actorRegistration = authentication != null && authentication.isAuthenticated()
                ? authentication.getName()
                : "SYSTEM";
        if (platformAccess != null && authentication != null && platformAccess.isPlatformAdmin(authentication.getName()))
            actorRegistration = "PLATFORM_SUPPORT";
        AuditEvent event = new AuditEvent(
                UUID.randomUUID(), organization, action, entityType, entityId, description, actorRegistration, Instant.now()
        );
        auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> findRecent() {
        UUID organizationId = organizationService.currentOrganization().getId();
        return auditEventRepository.findTop50ByOrganization_IdOrderByCreatedAtDesc(organizationId).stream()
                .map(AuditEventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventResponse> findPage(int page, int size) {
        UUID organizationId = organizationService.currentOrganization().getId();
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<AuditEvent> result = auditEventRepository.findByOrganization_Id(
                organizationId,
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResponse.from(result, result.getContent().stream().map(AuditEventResponse::from).toList());
    }
}
