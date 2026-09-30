package com.linepulse.auth;

import com.linepulse.common.ConflictException;
import com.linepulse.organization.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountOnboardingService {
    private final JdbcTemplate jdbc;
    private final OrganizationAccessService access;
    private final PasswordEncoder passwords;
    private final AccountEmailService emails;
    private final com.linepulse.audit.AuditService audit;
    public AccountOnboardingService(JdbcTemplate jdbc, OrganizationAccessService access, PasswordEncoder passwords, AccountEmailService emails, com.linepulse.audit.AuditService audit) {
        this.jdbc = jdbc; this.access = access; this.passwords = passwords; this.emails = emails; this.audit = audit;
    }
    @Transactional
    public void invite(AccountOnboardingController.Invite request) {
        var actor = access.requireCurrentRole(OrganizationRole.OWNER, OrganizationRole.ADMIN);
        if (request.role() == OrganizationRole.OWNER && actor.getRole() != OrganizationRole.OWNER)
            throw new AccessDeniedException("Apenas OWNER pode convidar outro OWNER.");
        inviteForOrganization(actor.getOrganization(), actor.getUser().getId(), request);
    }
    @Transactional
    public void inviteForOrganization(Organization organization, UUID actorId, AccountOnboardingController.Invite request) {
        // Platform provisioning only. The normal invite path has already checked membership roles.
        String registration = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        var owner = access.requireMembership(organization.getId(), registration, OrganizationRole.OWNER, OrganizationRole.ADMIN);
        if (!owner.getUser().getId().equals(actorId) || (request.role() == OrganizationRole.OWNER && owner.getRole() != OrganizationRole.OWNER))
            throw new AccessDeniedException("Convite não autorizado.");
        String invitedRegistration = request.registration().trim().toUpperCase(Locale.ROOT);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE LOWER(registration)=LOWER(?) OR LOWER(email)=?", Long.class, invitedRegistration, email) > 0)
            throw new ConflictException("Já existe uma conta com este cadastro ou e-mail.");
        UUID userId = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id,name,registration,password_hash,role,active,created_at,updated_at,email) VALUES (?,?,?,?,?,FALSE,NOW(),NOW(),?)",
                userId, request.name().trim(), invitedRegistration, passwords.encode(UUID.randomUUID().toString()), "OPERATOR", email);
        Long membershipId = jdbc.queryForObject("INSERT INTO organization_memberships (organization_id,user_id,role,active,created_at) VALUES (?,?,?,FALSE,NOW()) RETURNING id",
                Long.class, organization.getId(), userId, request.role().name());
        issue(userId, membershipId, actorId, email, "INVITE", 48 * 60);
        audit.recordForOrganization(organization, "ORGANIZATION_MEMBER_INVITED", "USER", userId, "Convite de membro enviado.");
    }
    @Transactional
    public void requestReset(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        LoginAttemptTracker.checkAllowed("RESET:" + normalized);
        LoginAttemptTracker.recordFailure("RESET:" + normalized);
        var users = jdbc.queryForList("SELECT id FROM users WHERE LOWER(email)=? AND active=TRUE", normalized);
        if (!users.isEmpty()) {
            try { issue((UUID) users.getFirst().get("id"), null, null, normalized, "RESET", 30); }
            catch (org.springframework.mail.MailException | IllegalArgumentException unavailable) {
                org.slf4j.LoggerFactory.getLogger(getClass()).warn("Account recovery email delivery unavailable");
            }
        }
    }
    private void issue(UUID userId, Long membershipId, UUID actor, String email, String purpose, int minutes) {
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("INSERT INTO account_tokens (token_hash,user_id,membership_id,issued_by,purpose,expires_at) VALUES (?,?,?,?,?,?)",
                hash(token), userId, membershipId, actor, purpose, Timestamp.from(Instant.now().plusSeconds(minutes * 60L)));
        emails.send(email, token, purpose.equals("INVITE"));
    }
    @Transactional
    public void finish(String token, String password) {
        String digest = hash(token);
        var initial = jdbc.queryForList("SELECT user_id FROM account_tokens WHERE token_hash=?", digest);
        if (initial.isEmpty()) throw new IllegalArgumentException("Link inválido ou expirado.");
        UUID userId = (UUID) initial.getFirst().get("user_id");
        jdbc.queryForList("SELECT id FROM users WHERE id=? FOR UPDATE", userId);
        var rows = jdbc.queryForList("SELECT * FROM account_tokens WHERE token_hash=? AND used_at IS NULL AND expires_at>NOW() FOR UPDATE", digest);
        if (rows.isEmpty()) throw new IllegalArgumentException("Link inválido ou expirado.");
        var row = rows.getFirst();
        boolean invite = row.get("purpose").equals("INVITE");
        if (invite) {
            int activated = jdbc.update("UPDATE organization_memberships target SET active=TRUE WHERE target.id=? AND EXISTS (SELECT 1 FROM organizations o WHERE o.id=target.organization_id AND o.active=TRUE AND (EXISTS (SELECT 1 FROM organization_memberships actor JOIN users u ON u.id=actor.user_id WHERE actor.organization_id=o.id AND actor.user_id=? AND actor.active=TRUE AND u.active=TRUE AND (actor.role='OWNER' OR (actor.role='ADMIN' AND target.role<>'OWNER'))) OR EXISTS (SELECT 1 FROM users u WHERE u.id=? AND u.active=TRUE AND u.platform_admin=TRUE)))", row.get("membership_id"), row.get("issued_by"), row.get("issued_by"));
            if (activated != 1) throw new IllegalArgumentException("Convite não está mais disponível.");
        } else if (jdbc.queryForObject("SELECT active FROM users WHERE id=?", Boolean.class, userId) != Boolean.TRUE) {
            throw new IllegalArgumentException("Link inválido ou expirado.");
        }
        jdbc.update("UPDATE users SET password_hash=?, active=TRUE, credentials_changed_at=NOW(), updated_at=NOW() WHERE id=?", passwords.encode(password), userId);
        jdbc.update("UPDATE account_tokens SET used_at=NOW() WHERE user_id=? AND used_at IS NULL", userId);
    }
    static String hash(String token) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
