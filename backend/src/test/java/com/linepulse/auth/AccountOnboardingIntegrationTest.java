package com.linepulse.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class AccountOnboardingIntegrationTest {
    @Container @ServiceConnection static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @Autowired AccountOnboardingService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;
    @MockitoBean AccountEmailService emails;
    private UUID user() {
        UUID id = UUID.randomUUID();
        String registration = "TEST" + id.toString().substring(0,8);
        jdbc.update("INSERT INTO users (id,name,registration,password_hash,role,active,created_at,updated_at,email) VALUES (?, 'Teste', ?, ?, 'OPERATOR',TRUE,NOW(),NOW(),?)", id, registration, passwords.encode("Original12345!"), id+"@example.test");
        return id;
    }
    private String token(UUID id, boolean expired) {
        String raw = UUID.randomUUID().toString() + "abcdefg";
        jdbc.update("INSERT INTO account_tokens (token_hash,user_id,purpose,expires_at) VALUES (?,?,'RESET',?)", AccountOnboardingService.hash(raw), id, java.sql.Timestamp.from(Instant.now().plusSeconds(expired ? -60 : 600)));
        return raw;
    }
    @Test void recoveryUsesHashedSingleUseTokenAndInvalidatesAllOutstandingTokens() {
        UUID id = user(); String first = token(id,false), second = token(id,false);
        String previousJwt = jwt.generate(users.findById(id).orElseThrow());
        service.finish(first,"Replacement123!");
        var user = users.findById(id).orElseThrow();
        assertTrue(passwords.matches("Replacement123!",user.getPasswordHash()));
        assertNotNull(user.getCredentialsChangedAt());
        assertNotEquals(user.getCredentialsChangedAt().toString(),jwt.parse(previousJwt).get("credentialsVersion"));
        assertEquals(user.getCredentialsChangedAt().toString(),jwt.parse(jwt.generate(user)).get("credentialsVersion"));
        assertThrows(IllegalArgumentException.class,()->service.finish(first,"Another123456!"));
        assertThrows(IllegalArgumentException.class,()->service.finish(second,"Another123456!"));
    }
    @Test void invitationActivatesOnlyIntendedMembershipAndCannotBeReplayed() {
        UUID actorId = user();
        var actor = users.findById(actorId).orElseThrow();
        UUID organizationId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        jdbc.update("INSERT INTO organization_memberships (organization_id,user_id,role,active,created_at) VALUES (?,?,'OWNER',TRUE,NOW())",organizationId,actorId);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(actor.getRegistration(),null,java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));
        String suffix = UUID.randomUUID().toString().substring(0,8);
        String email = suffix+"@example.test";
        var captured = org.mockito.ArgumentCaptor.forClass(String.class);
        try {
            service.invite(new AccountOnboardingController.Invite("Convidado","INV"+suffix,email,com.linepulse.organization.OrganizationRole.TECHNICIAN));
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
        verify(emails).send(eq(email),captured.capture(),eq(true));
        var invited = users.findByRegistrationIgnoreCase("INV"+suffix).orElseThrow();
        assertFalse(invited.isActive());
        service.finish(captured.getValue(),"Invitation123!");
        assertTrue(users.findById(invited.getId()).orElseThrow().isActive());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM organization_memberships WHERE user_id=? AND organization_id=? AND active=TRUE AND role='TECHNICIAN'",Integer.class,invited.getId(),organizationId));
        assertThrows(IllegalArgumentException.class,()->service.finish(captured.getValue(),"OtherPassword123!"));
    }
    @Test void expiredTokenDoesNotChangePassword() {
        UUID id = user(); String raw = token(id,true);
        assertThrows(IllegalArgumentException.class,()->service.finish(raw,"Replacement123!"));
        assertTrue(passwords.matches("Original12345!",users.findById(id).orElseThrow().getPasswordHash()));
    }
    @Test void resetForUnknownEmailDoesNotSendAnything() {
        clearInvocations(emails);
        service.requestReset(UUID.randomUUID()+"@example.test");
        verifyNoInteractions(emails);
    }
    @Test void recoveryStoresDigestAndSendsTokenOnlyToVerifiedAccountEmail() {
        UUID id = user();
        var captured = org.mockito.ArgumentCaptor.forClass(String.class);
        service.requestReset(id+"@example.test");
        verify(emails).send(eq(id+"@example.test"),captured.capture(),eq(false));
        assertEquals(43,captured.getValue().length());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM account_tokens WHERE token_hash=?",Integer.class,AccountOnboardingService.hash(captured.getValue())));
    }
}
