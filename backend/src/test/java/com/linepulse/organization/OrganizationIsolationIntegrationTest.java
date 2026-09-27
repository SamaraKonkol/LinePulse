package com.linepulse.organization;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
class OrganizationIsolationIntegrationTest {
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
        UserAccount companyAAdmin = createUser("Admin Empresa A", "ADMA01");
        UserAccount companyBAdmin = createUser("Admin Empresa B", "ADMB01");

        Organization companyB = organizationRepository.save(new Organization(
                UUID.randomUUID(),
                "Empresa B",
                "empresa-b",
                OrganizationType.COMPANY,
                true,
                Instant.now()
        ));
        membershipRepository.save(new OrganizationMembership(companyB, companyBAdmin, OrganizationRole.OWNER, true, Instant.now()));
    }

    @Test
    void shouldKeepIndustrialStructureIsolatedBetweenOrganizations() throws Exception {
        String tokenA = login("ADMA01");
        String tokenB = login("ADMB01");

        String plantAResponse = mockMvc.perform(post("/api/admin/structure/plants")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Planta Empresa A\",\"code\":\"A01\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/api/admin/structure/plants")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Planta Empresa B\",\"code\":\"B01\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/structure/plants")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("A01")))
                .andExpect(content().string(not(containsString("B01"))));

        mockMvc.perform(get("/api/admin/structure/plants")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("B01")))
                .andExpect(content().string(not(containsString("A01"))));

        UUID plantAId = UUID.fromString(objectMapper.readTree(plantAResponse).get("id").asText());
        mockMvc.perform(patch("/api/admin/structure/plants/{id}", plantAId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tentativa indevida\",\"code\":\"X01\"}"))
                .andExpect(status().isNotFound());
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

    private UserAccount createUser(String name, String registration) {
        Instant now = Instant.now();
        return userRepository.save(new UserAccount(
                UUID.randomUUID(),
                name,
                registration,
                passwordEncoder.encode("TestPass123!"),
                UserRole.ADMIN,
                true,
                now,
                now
        ));
    }
}
