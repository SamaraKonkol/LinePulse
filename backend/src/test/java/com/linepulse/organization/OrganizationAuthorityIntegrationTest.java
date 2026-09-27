package com.linepulse.organization;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linepulse.auth.LoginRequest;
import com.linepulse.auth.UserAccount;
import com.linepulse.auth.UserRepository;
import com.linepulse.auth.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OrganizationAuthorityIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private OrganizationMembershipRepository membershipRepository;

    private Organization operatorOrganization;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        Instant now = Instant.now();
        UserAccount user = userRepository.save(new UserAccount(
                UUID.randomUUID(), "Usuário Multiempresa", "MULTI01", passwordEncoder.encode("TestPass123!"),
                UserRole.ADMIN, true, now, now
        ));

        Organization adminOrganization = organizationRepository.save(new Organization(
                UUID.randomUUID(), "Empresa Admin", "empresa-admin", OrganizationType.COMPANY, true, now
        ));
        operatorOrganization = organizationRepository.save(new Organization(
                UUID.randomUUID(), "Empresa Operador", "empresa-operador", OrganizationType.COMPANY, true, now
        ));
        membershipRepository.save(new OrganizationMembership(adminOrganization, user, OrganizationRole.OWNER, true, now));
        membershipRepository.save(new OrganizationMembership(operatorOrganization, user, OrganizationRole.OPERATOR, true, now));
    }

    @Test
    void shouldUseSelectedOrganizationRoleInsteadOfGlobalJwtRole() throws Exception {
        String token = login();

        mockMvc.perform(post("/api/admin/structure/plants")
                        .header("Authorization", "Bearer " + token)
                        .header(OrganizationService.ORGANIZATION_HEADER, operatorOrganization.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tentativa indevida\",\"code\":\"NOPE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectUnknownWorkspaceHeaderBeforeAdminAuthorization() throws Exception {
        String token = login();

        mockMvc.perform(post("/api/admin/structure/plants")
                        .header("Authorization", "Bearer " + token)
                        .header(OrganizationService.ORGANIZATION_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tentativa indevida\",\"code\":\"NOPE\"}"))
                .andExpect(status().isForbidden());
    }

    private String login() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("MULTI01", "TestPass123!"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode payload = objectMapper.readTree(response);
        return payload.get("token").asText();
    }
}
