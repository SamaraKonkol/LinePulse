package com.linepulse.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class AuthenticationAuthorizationIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        createUser("Operador Teste", "OP1001", UserRole.OPERATOR);
        createUser("Técnico Teste", "TEC1001", UserRole.TECHNICIAN);
        createUser("Administrador Teste", "ADM1001", UserRole.ADMIN);
    }

    @Test
    void shouldBlockPublicRegistration() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Nova Pessoa",
                                  "registration": "OP2001",
                                  "password": "StrongPass123!"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowOnlyAdminToCreateUsers() throws Exception {
        String operatorToken = login("OP1001", "TestPass123!");
        String technicianToken = login("TEC1001", "TestPass123!");
        String adminToken = login("ADM1001", "TestPass123!");
        String body = """
                {
                  "name": "Novo Técnico",
                  "registration": "TEC2001",
                  "password": "StrongPass123!",
                  "role": "TECHNICIAN"
                }
                """;

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + technicianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registration").value("TEC2001"))
                .andExpect(jsonPath("$.role").value("TECHNICIAN"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldEnforceAuditAuthorizationUsingRealJwt() throws Exception {
        String operatorToken = login("OP1001", "TestPass123!");
        String technicianToken = login("TEC1001", "TestPass123!");

        mockMvc.perform(get("/api/audit-events")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/audit-events")
                        .header("Authorization", "Bearer " + technicianToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRestrictAdministrationToAdmins() throws Exception {
        String operatorToken = login("OP1001", "TestPass123!");
        String technicianToken = login("TEC1001", "TestPass123!");
        String adminToken = login("ADM1001", "TestPass123!");

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + technicianToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAdminToPromoteARegularUser() throws Exception {
        String adminToken = login("ADM1001", "TestPass123!");
        UserAccount operator = userRepository.findByRegistrationIgnoreCase("OP1001").orElseThrow();

        mockMvc.perform(patch("/api/admin/users/{userId}/role", operator.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"TECHNICIAN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TECHNICIAN"));
    }

    @Test
    void shouldAllowAdminToDeleteARegularUserAndInvalidateTheirToken() throws Exception {
        String operatorToken = login("OP1001", "TestPass123!");
        String adminToken = login("ADM1001", "TestPass123!");
        UserAccount operator = userRepository.findByRegistrationIgnoreCase("OP1001").orElseThrow();

        mockMvc.perform(delete("/api/admin/users/{userId}", operator.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden());
    }

    private String login(String registration, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(registration, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode payload = objectMapper.readTree(response);
        return payload.get("token").asText();
    }

    private void createUser(String name, String registration, UserRole role) {
        Instant now = Instant.now();
        userRepository.save(new UserAccount(
                UUID.randomUUID(),
                name,
                registration,
                passwordEncoder.encode("TestPass123!"),
                role,
                true,
                now,
                now
        ));
    }
}
