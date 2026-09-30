package com.linepulse.organization;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class OrganizationAuthorityFilter extends OncePerRequestFilter {
    @org.springframework.beans.factory.annotation.Autowired
    private com.linepulse.platform.PlatformAccess platformAccess;
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final OrganizationMembershipRepository membershipRepository;

    public OrganizationAuthorityFilter(OrganizationMembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String registration = authentication.getName();
        if (platformAccess != null && platformAccess.isPlatformAdmin(registration)) {
            // JWT authentication must have verified the second factor before granting platform access.
            try {
                platformAccess.requireAdmin();
                List<SimpleGrantedAuthority> granted = new java.util.ArrayList<>();
                granted.add(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));
                var selected = selectedOrganizationId(request);
                if (!isOrganizationHeaderAbsent(request)) {
                    var support = platformAccess.supportMembership(selected.orElse(null), registration);
                    granted.add(new SimpleGrantedAuthority(authorityFor(support)));
                    String detail = request.getMethod() + " " + request.getRequestURI() + " role=" + support.getRole();
                    jdbc.update("INSERT INTO platform_audit_events (actor_registration,organization_id,action,detail) VALUES (?,?,'WORKSPACE_ACCESS',?)",
                            registration, support.getOrganization().getId(), detail.substring(0, Math.min(500, detail.length())));
                }
                var scoped = new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), authentication.getCredentials(), granted);
                SecurityContextHolder.getContext().setAuthentication(scoped);
            } catch (org.springframework.security.access.AccessDeniedException denied) {
                response.sendError(403, "Workspace ou acesso de suporte inválido."); return;
            }
            filterChain.doFilter(request, response); return;
        }
        Optional<OrganizationMembership> membership = selectedOrganizationId(request)
                .flatMap(id -> membershipRepository.findByOrganization_IdAndUser_RegistrationIgnoreCaseAndActiveTrue(id, registration));
        if (membership.isEmpty() && isOrganizationHeaderAbsent(request)) {
            membership = membershipRepository.findFirstByUser_RegistrationIgnoreCaseAndActiveTrueOrderByCreatedAtAsc(registration);
        }

        List<SimpleGrantedAuthority> authorities = membership
                .filter(candidate -> candidate.getOrganization().isActive())
                .map(candidate -> List.of(new SimpleGrantedAuthority(authorityFor(candidate))))
                .orElseGet(List::of);

        var scopedAuthentication = new UsernamePasswordAuthenticationToken(
                authentication.getPrincipal(), authentication.getCredentials(), authorities
        );
        scopedAuthentication.setDetails(authentication.getDetails());
        SecurityContextHolder.getContext().setAuthentication(scopedAuthentication);
        filterChain.doFilter(request, response);
    }

    private String authorityFor(OrganizationMembership membership) {
        if (membership.getRole() == OrganizationRole.OPERATOR) {
            return "ROLE_OPERATOR";
        }
        if (membership.getOrganization().getType() == OrganizationType.SERVICE_PROVIDER) {
            return "ROLE_TECHNICIAN";
        }
        return switch (membership.getRole()) {
            case OWNER, ADMIN -> "ROLE_ADMIN";
            case TECHNICIAN, MECHANIC -> "ROLE_TECHNICIAN";
            case OPERATOR -> "ROLE_OPERATOR";
        };
    }

    private Optional<UUID> selectedOrganizationId(HttpServletRequest request) {
        String value = request.getHeader(OrganizationService.ORGANIZATION_HEADER);
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private boolean isOrganizationHeaderAbsent(HttpServletRequest request) {
        String value = request.getHeader(OrganizationService.ORGANIZATION_HEADER);
        return value == null || value.isBlank();
    }
}
