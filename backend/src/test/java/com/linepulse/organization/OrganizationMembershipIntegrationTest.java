package com.linepulse.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class OrganizationMembershipIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private OrganizationMembershipRepository membershipRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        createUser("Administrador Teste", "ADM3001", UserRole.ADMIN);
    }

    @Test
    void shouldBootstrapDefaultOrganizationOnFirstLegacyLogin() throws Exception {
        String token = login("ADM3001", "TestPass123!");

        mockMvc.perform(get("/api/organizations/my").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("linepulse-default"))
                .andExpect(jsonPath("$[0].type").value("COMPANY"))
                .andExpect(jsonPath("$[0].role").value("OWNER"));
    }

    @Test
    void shouldAssignAdminCreatedUserToTheSameOrganization() throws Exception {
        String adminToken = login("ADM3001", "TestPass123!");

        createUserThroughAdmin(adminToken, null, "TEC3001", "TECHNICIAN");

        String technicianToken = login("TEC3001", "StrongPass123!");
        mockMvc.perform(get("/api/organizations/my").header("Authorization", "Bearer " + technicianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("linepulse-default"))
                .andExpect(jsonPath("$[0].role").value("TECHNICIAN"));
    }

    @Test
    void shouldAssignLegacyAdminCreatedUserToSelectedWorkspace() throws Exception {
        String adminToken = login("ADM3001", "TestPass123!");
        UserAccount admin = userRepository.findByRegistrationIgnoreCase("ADM3001").orElseThrow();
        Instant now = Instant.now();
        Organization secondCompany = organizationRepository.save(new Organization(
                UUID.randomUUID(), "Empresa Selecionada", "empresa-selecionada", OrganizationType.COMPANY, true, now
        ));
        membershipRepository.save(new OrganizationMembership(secondCompany, admin, OrganizationRole.OWNER, true, now));

        createUserThroughAdmin(adminToken, secondCompany.getId(), "TEC3002", "TECHNICIAN");

        String technicianToken = login("TEC3002", "StrongPass123!");
        mockMvc.perform(get("/api/organizations/my").header("Authorization", "Bearer " + technicianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("empresa-selecionada"))
                .andExpect(jsonPath("$[0].role").value("TECHNICIAN"));
    }

    @Test
    void shouldCreateLegacyAdminAsOrganizationAdminInsteadOfOwner() throws Exception {
        String adminToken = login("ADM3001", "TestPass123!");

        createUserThroughAdmin(adminToken, null, "ADM3002", "ADMIN");

        Organization defaultOrganization = organizationRepository.findBySlugIgnoreCase(OrganizationService.DEFAULT_SLUG).orElseThrow();
        OrganizationMembership membership = membershipRepository
                .findByOrganization_IdAndUser_RegistrationIgnoreCaseAndActiveTrue(defaultOrganization.getId(), "ADM3002")
                .orElseThrow();
        assertThat(membership.getRole()).isEqualTo(OrganizationRole.ADMIN);
    }

    @Test
    void shouldKeepLegacyAdminUserListingAndMutationsInsideSelectedOrganization() throws Exception {
        String adminToken = login("ADM3001", "TestPass123!");
        Instant now = Instant.now();
        Organization otherCompany = organizationRepository.save(new Organization(
                UUID.randomUUID(), "Empresa B", "empresa-b-isolada", OrganizationType.COMPANY, true, now
        ));
        UserAccount foreignUser = createUser("Operador Empresa B", "OPB3001", UserRole.OPERATOR);
        OrganizationMembership foreignMembership = membershipRepository.save(
                new OrganizationMembership(otherCompany, foreignUser, OrganizationRole.OPERATOR, true, now)
        );

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.registration == 'OPB3001')]").isEmpty());

        mockMvc.perform(patch("/api/admin/users/{userId}/role", foreignUser.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isNotFound());

        OrganizationMembership unchanged = membershipRepository.findById(foreignMembership.getId()).orElseThrow();
        assertThat(unchanged.getRole()).isEqualTo(OrganizationRole.OPERATOR);
    }

    @Test
    void shouldNotRecreateDefaultMembershipWhenExistingMembershipIsInactive() throws Exception {
        UserAccount admin = userRepository.findByRegistrationIgnoreCase("ADM3001").orElseThrow();
        Instant now = Instant.now();
        Organization company = organizationRepository.save(new Organization(
                UUID.randomUUID(), "Empresa Suspensa", "empresa-suspensa", OrganizationType.COMPANY, true, now
        ));
        membershipRepository.save(new OrganizationMembership(company, admin, OrganizationRole.OWNER, false, now));

        String token = login("ADM3001", "TestPass123!");

        mockMvc.perform(get("/api/organizations/my").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        assertThat(membershipRepository.existsByUser_RegistrationIgnoreCase("ADM3001")).isTrue();
        assertThat(membershipRepository.findByUser_RegistrationIgnoreCaseAndActiveTrueOrderByOrganization_NameAsc("ADM3001")).isEmpty();
    }

    private void createUserThroughAdmin(String token, UUID organizationId, String registration, String role) throws Exception {
        var request = post("/api/admin/users")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Usuário da Empresa",
                          "registration": "%s",
                          "password": "StrongPass123!",
                          "role": "%s"
                        }
                        """.formatted(registration, role));
        if (organizationId != null) {
            request.header(OrganizationService.ORGANIZATION_HEADER, organizationId.toString());
        }
        mockMvc.perform(request).andExpect(status().isCreated());
    }

    private String login(String registration, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(registration, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode payload = objectMapper.readTree(response);
        return payload.get("token").asText();
    }

    private UserAccount createUser(String name, String registration, UserRole role) {
        Instant now = Instant.now();
        return userRepository.save(new UserAccount(
                UUID.randomUUID(), name, registration, passwordEncoder.encode("TestPass123!"), role, true, now, now
        ));
    }
}
