package com.linepulse.asset;

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
class IndustrialAdministrationIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        createUser("Técnico Teste", "TEC1001", UserRole.TECHNICIAN);
        createUser("Administrador Teste", "ADM1001", UserRole.ADMIN);
    }

    @Test
    void shouldRestrictStructureAdministrationToAdmin() throws Exception {
        String technicianToken = login("TEC1001");
        String adminToken = login("ADM1001");
        String body = "{\"name\":\"Planta B\",\"code\":\"PB\"}";

        mockMvc.perform(post("/api/admin/structure/plants").header("Authorization", "Bearer " + technicianToken).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/structure/plants").header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("PB"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldReserveMachineDeactivationForAdmin() throws Exception {
        String technicianToken = login("TEC1001");
        String adminToken = login("ADM1001");
        String machineId = "40000000-0000-0000-0000-000000000001";
        String inactive = "{\"status\":\"INACTIVE\"}";
        String running = "{\"status\":\"RUNNING\"}";

        mockMvc.perform(patch("/api/machines/{machineId}/status", machineId).header("Authorization", "Bearer " + technicianToken).contentType(MediaType.APPLICATION_JSON).content(inactive))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/machines/{machineId}/status", machineId).header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON).content(inactive))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(patch("/api/machines/{machineId}/status", machineId).header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON).content(running))
                .andExpect(status().isOk());
    }

    private String login(String registration) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(registration, "TestPass123!"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode payload = objectMapper.readTree(response);
        return payload.get("token").asText();
    }

    private void createUser(String name, String registration, UserRole role) {
        Instant now = Instant.now();
        userRepository.save(new UserAccount(UUID.randomUUID(), name, registration, passwordEncoder.encode("TestPass123!"), role, true, now, now));
    }
}
