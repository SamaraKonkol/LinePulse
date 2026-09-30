package com.linepulse.platform;

import com.linepulse.auth.UserRepository;
import com.linepulse.organization.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class PlatformAccess {
    public static final String ROLE_HEADER = "X-LinePulse-Support-Role";
    private final UserRepository users;
    private final OrganizationRepository organizations;
    public PlatformAccess(UserRepository users, OrganizationRepository organizations) {
        this.users = users; this.organizations = organizations;
    }
    public boolean isPlatformAdmin(String registration) {
        return users.findByRegistrationIgnoreCase(registration)
                .filter(user -> user.isActive() && user.isPlatformAdmin()).isPresent();
    }
    public String requireAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() ||
                auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_PLATFORM_ADMIN")) ||
                !isPlatformAdmin(auth.getName())) throw new AccessDeniedException("Acesso exclusivo da plataforma.");
        return auth.getName();
    }
    public OrganizationMembership supportMembership(UUID organizationId, String registration) {
        requireAdmin();
        if (organizationId == null) throw new AccessDeniedException("Selecione um workspace para suporte.");
        var org = organizations.findById(organizationId).filter(Organization::isActive)
                .orElseThrow(() -> new AccessDeniedException("Workspace indisponível."));
        OrganizationRole role = OrganizationRole.OWNER;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String value = attributes.getRequest().getHeader(ROLE_HEADER);
            if (value != null && !value.isBlank()) {
                try { role = OrganizationRole.valueOf(value); }
                catch (IllegalArgumentException invalid) { throw new AccessDeniedException("Papel de suporte inválido."); }
            }
        }
        // This membership is transient: platform staff never become tenant team members.
        return new OrganizationMembership(org, users.findByRegistrationIgnoreCase(registration).orElseThrow(), role, true, Instant.now());
    }
}
