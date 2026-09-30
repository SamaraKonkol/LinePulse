package com.linepulse.platform;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linepulse.auth.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties="PLATFORM_TOTP_SECRET=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")
@AutoConfigureMockMvc @Testcontainers
class PlatformAdministrationIntegrationTest {
    @Container @ServiceConnection static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;
    @Autowired PlatformTotp totp;
    @MockitoBean AccountEmailService emails;
    final UUID defaultOrg = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final String seed = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
    @BeforeEach void setup() {
        users.deleteAll(); reset(emails);
        new PlatformBootstrap(jdbc,passwords,totp,"founder@example.test","BootstrapPassword123!").run(null);
    }
    String loginPlatform() throws Exception {
        var result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("registration","PLATFORM001","password","BootstrapPassword123!","otp",PlatformTotp.codeAt(seed,Instant.now().getEpochSecond()/30)))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.platformAdmin").value(true)).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
    @Test void bootstrapIsIdempotentAndCannotPromoteAnExistingAccount() {
        new PlatformBootstrap(jdbc,passwords,totp,"founder@example.test","DifferentPassword123!").run(null);
        assertTrue(passwords.matches("BootstrapPassword123!",users.findByRegistrationIgnoreCase("PLATFORM001").orElseThrow().getPasswordHash()));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM organization_memberships",Integer.class));
        assertThrows(IllegalStateException.class,()->new PlatformBootstrap(jdbc,passwords,totp,"other@example.test","Password12345!").run(null));
        users.deleteAll();
        jdbc.update("INSERT INTO users (id,name,registration,password_hash,role,active,email,created_at,updated_at) VALUES (?,'Existing','PLATFORM001',?,'OPERATOR',TRUE,'founder@example.test',NOW(),NOW())",UUID.randomUUID(),passwords.encode("OriginalPassword123!"));
        assertThrows(IllegalStateException.class,()->new PlatformBootstrap(jdbc,passwords,totp,"founder@example.test","Password12345!").run(null));
        assertFalse(users.findByRegistrationIgnoreCase("PLATFORM001").orElseThrow().isPlatformAdmin());
    }
    @Test void loginRequiresSecondFactorAndRejectsReplayAndPrePromotionJwt() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"registration\":\"PLATFORM001\",\"password\":\"BootstrapPassword123!\"}")).andExpect(status().isUnauthorized());
        String token = loginPlatform();
        mvc.perform(get("/api/platform/organizations").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("registration","PLATFORM001","password","BootstrapPassword123!","otp",PlatformTotp.codeAt(seed,Instant.now().getEpochSecond()/30))))).andExpect(status().isUnauthorized());
        var user = users.save(new UserAccount(UUID.randomUUID(),"Legacy","LEGACY",passwords.encode("Password12345!"),UserRole.ADMIN,true,Instant.now(),Instant.now()));
        String old = jwt.generate(user);
        jdbc.update("UPDATE users SET platform_admin=TRUE WHERE id=?",user.getId());
        mvc.perform(get("/api/platform/organizations").header("Authorization","Bearer "+old)).andExpect(status().isForbidden());
    }
    @Test void provisioningInvitesOwnerAndSupportStaysInvisibleScopedAndAudited() throws Exception {
        String token = loginPlatform(); String suffix = UUID.randomUUID().toString().substring(0,8);
        String response = mvc.perform(post("/api/platform/organizations").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("name","Company Test","slug","test-"+suffix,"type","COMPANY","ownerName","Owner","ownerRegistration","OWNER"+suffix,"ownerEmail",suffix+"@example.test"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String organization = json.readTree(response).get("id").asText();
        var capture = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(emails).send(eq(suffix+"@example.test"),capture.capture(),eq(true));
        mvc.perform(post("/api/auth/complete-account").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("token",capture.getValue(),"password","OwnerPassword123!")))).andExpect(status().isOk());
        mvc.perform(get("/api/organization-members").header("Authorization","Bearer "+token).header("X-LinePulse-Organization",organization))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].registration").value("OWNER"+suffix));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM organization_memberships m JOIN users u ON u.id=m.user_id WHERE u.platform_admin=TRUE",Integer.class));
        mvc.perform(get("/api/organizations/current").header("Authorization","Bearer "+token).header("X-LinePulse-Organization",organization).header("X-LinePulse-Support-Role","OPERATOR"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("OPERATOR"));
        mvc.perform(get("/api/admin/users").header("Authorization","Bearer "+token).header("X-LinePulse-Organization",organization).header("X-LinePulse-Support-Role","OPERATOR")).andExpect(status().isForbidden());
        mvc.perform(get("/api/dashboard").header("Authorization","Bearer "+token).header("X-LinePulse-Organization",UUID.randomUUID())).andExpect(status().isForbidden());
        mvc.perform(get("/api/dashboard").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM platform_audit_events WHERE action='WORKSPACE_ACCESS' AND organization_id=?",Integer.class,UUID.fromString(organization))>0);
        String ownerToken = jwt.generate(users.findByRegistrationIgnoreCase("OWNER"+suffix).orElseThrow());
        mvc.perform(get("/api/platform/organizations").header("Authorization","Bearer "+ownerToken)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/users/"+users.findByRegistrationIgnoreCase("PLATFORM001").orElseThrow().getId()+"/status")
                .header("Authorization","Bearer "+ownerToken).header("X-LinePulse-Organization",organization)
                .contentType(MediaType.APPLICATION_JSON).content("{\"active\":false}")).andExpect(status().isNotFound());
        String newRegistration = "NORMAL"+suffix;
        mvc.perform(post("/api/admin/users").header("Authorization","Bearer "+ownerToken).header("X-LinePulse-Organization",organization)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("name","Normal","registration",newRegistration,"password","NormalPassword123!","role","ADMIN","platformAdmin",true))))
                .andExpect(status().isCreated());
        assertFalse(users.findByRegistrationIgnoreCase(newRegistration).orElseThrow().isPlatformAdmin());
        mvc.perform(get("/api/dashboard").header("Authorization","Bearer "+ownerToken).header("X-LinePulse-Organization",defaultOrg)).andExpect(status().isForbidden());
    }
    @Test void failedOwnerEmailRollsBackOrganizationAndAccount() throws Exception {
        String token = loginPlatform(); String suffix = UUID.randomUUID().toString().substring(0,8);
        doThrow(new org.springframework.mail.MailSendException("unavailable")).when(emails).send(eq(suffix+"@example.test"),anyString(),eq(true));
        mvc.perform(post("/api/platform/organizations").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("name","Failed","slug","failed-"+suffix,"type","SERVICE_PROVIDER","ownerName","Owner","ownerRegistration","FAIL"+suffix,"ownerEmail",suffix+"@example.test")))).andExpect(status().is5xxServerError());
        assertFalse(users.existsByRegistrationIgnoreCase("FAIL"+suffix));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM organizations WHERE slug=?",Integer.class,"failed-"+suffix));
    }
}
