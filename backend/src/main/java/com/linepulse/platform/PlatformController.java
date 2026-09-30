package com.linepulse.platform;

import com.linepulse.auth.*;
import com.linepulse.organization.*;
import com.linepulse.common.ConflictException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/platform")
public class PlatformController {
    private final PlatformAccess access;
    private final OrganizationRepository organizations;
    private final UserRepository users;
    private final AccountOnboardingService onboarding;
    private final JdbcTemplate jdbc;
    public PlatformController(PlatformAccess access, OrganizationRepository organizations, UserRepository users,
            AccountOnboardingService onboarding, JdbcTemplate jdbc) {
        this.access = access; this.organizations = organizations; this.users = users; this.onboarding = onboarding; this.jdbc = jdbc;
    }
    public record Provision(@NotBlank @Size(max=160) String name,
            @NotBlank @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max=120) String slug,
            @NotNull OrganizationType type, @NotBlank @Size(max=120) String ownerName,
            @NotBlank @Size(max=40) String ownerRegistration,
            @NotBlank @Email @Size(max=254) String ownerEmail) {}
    public record OrganizationView(UUID id, String name, String slug, OrganizationType type, boolean active) {
        static OrganizationView from(Organization org) { return new OrganizationView(org.getId(),org.getName(),org.getSlug(),org.getType(),org.isActive()); }
    }
    @GetMapping("/organizations")
    public List<OrganizationView> list() {
        access.requireAdmin();
        return organizations.findAll(org.springframework.data.domain.Sort.by("name")).stream().map(OrganizationView::from).toList();
    }
    @PostMapping("/organizations") @Transactional
    public OrganizationView create(@Valid @RequestBody Provision request) {
        String actor = access.requireAdmin();
        if (organizations.findBySlugIgnoreCase(request.slug()).isPresent()) throw new ConflictException("Este identificador de organização já existe.");
        var organization = organizations.saveAndFlush(new Organization(UUID.randomUUID(),request.name().trim(),request.slug(),request.type(),true,Instant.now()));
        onboarding.inviteForOrganization(organization,users.findByRegistrationIgnoreCase(actor).orElseThrow().getId(),
                new AccountOnboardingController.Invite(request.ownerName(),request.ownerRegistration(),request.ownerEmail(),OrganizationRole.OWNER));
        jdbc.update("INSERT INTO platform_audit_events (actor_registration,organization_id,action,detail) VALUES (?,?,'ORGANIZATION_PROVISIONED','Organização criada e proprietário convidado.')",actor,organization.getId());
        return OrganizationView.from(organization);
    }
    @GetMapping("/audit-events")
    public List<Map<String,Object>> audit(@RequestParam(defaultValue="0") int page) {
        access.requireAdmin();
        return jdbc.queryForList("SELECT id,actor_registration,organization_id,action,detail,created_at FROM platform_audit_events ORDER BY created_at DESC,id DESC LIMIT 100 OFFSET ?", Math.min(Math.max(page,0),10000)*100);
    }
}
