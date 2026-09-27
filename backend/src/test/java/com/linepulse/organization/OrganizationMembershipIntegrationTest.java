package com.linepulse.organization;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        createUser("Administrador Teste", "ADM3001", UserRole.ADMIN);
    }

    @Test
    void shouldBootstrapDefaultOrganizationOnLogin() throws Exception {
        String token = login("ADM3001", "TestPass123!");

        mockMvc.perform(get("/api/organizations/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("linepulse-default"))
                .andExpect(jsonPath("$[0].type").value("COMPANY"))
                .andExpect(jsonPath("$[0].role").value("OWNER"));
    }

    @Test
    void shouldAssignAdminCreatedUserToTheSameOrganization() throws Exception {
        String adminToken = login("ADM3001", "TestPass123!");

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Técnico da Empresa",
                                  "registration": "TEC3001",
                                  "password": "StrongPass123!",
                                  "role": "TECHNICIAN"
                                }
                                """))
                .andExpect(status().isCreated());

        String technicianToken = login("TEC3001", "StrongPass123!");
        mockMvc.perform(get("/api/organizations/my")
                        .header("Authorization", "Bearer " + technicianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("linepulse-default"))
                .andExpect(jsonPath("$[0].role").value("TECHNICIAN"));
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
