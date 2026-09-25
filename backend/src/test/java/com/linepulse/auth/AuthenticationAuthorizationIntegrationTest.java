package com.linepulse.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        createUser("Operador Teste", "operator@test.local", UserRole.OPERATOR);
        createUser("Técnico Teste", "technician@test.local", UserRole.TECHNICIAN);
    }

    @Test
    void shouldCreateNewRegistrationsAsOperator() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Nova Pessoa",
                                  "email": "new.user@test.local",
                                  "password": "StrongPass123!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("OPERATOR"));
    }

    @Test
    void shouldEnforceAuditAuthorizationUsingRealJwt() throws Exception {
        String operatorToken = login("operator@test.local", "TestPass123!");
        String technicianToken = login("technician@test.local", "TestPass123!");

        mockMvc.perform(get("/api/audit-events")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/audit-events")
                        .header("Authorization", "Bearer " + technicianToken))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectTokenAfterUserIsRemoved() throws Exception {
        String token = login("operator@test.local", "TestPass123!");
        UserAccount operator = userRepository.findByEmailIgnoreCase("operator@test.local").orElseThrow();
        userRepository.delete(operator);

        mockMvc.perform(get("/api/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode payload = objectMapper.readTree(response);
        return payload.get("token").asText();
    }

    private void createUser(String name, String email, UserRole role) {
        Instant now = Instant.now();
        userRepository.save(new UserAccount(
                UUID.randomUUID(),
                name,
                email,
                passwordEncoder.encode("TestPass123!"),
                role,
                true,
                now,
                now
        ));
    }
}
