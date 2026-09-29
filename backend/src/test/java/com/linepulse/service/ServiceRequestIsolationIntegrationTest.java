package com.linepulse.service;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linepulse.auth.LoginRequest;
import com.linepulse.auth.UserAccount;
import com.linepulse.auth.UserRepository;
import com.linepulse.auth.UserRole;
import com.linepulse.organization.Organization;
import com.linepulse.organization.OrganizationMembership;
import com.linepulse.organization.OrganizationMembershipRepository;
import com.linepulse.organization.OrganizationRepository;
import com.linepulse.organization.OrganizationRole;
import com.linepulse.organization.OrganizationType;
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
class ServiceRequestIsolationIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private OrganizationMembershipRepository membershipRepository;

    private Organization providerA;
    private Organization providerB;
    private UUID mechanicId;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        createUser("Admin Empresa", "CMP001", UserRole.ADMIN);
        UserAccount companyBOwner = createUser("Responsável Empresa B", "CMP002", UserRole.OPERATOR);
        Organization companyB = organizationRepository.save(new Organization(UUID.randomUUID(), "Empresa B", "company-b-e2e", OrganizationType.COMPANY, true, Instant.now()));
        membershipRepository.save(new OrganizationMembership(companyB, companyBOwner, OrganizationRole.OWNER, true, Instant.now()));
        UserAccount providerAOwner = createUser("Responsável A", "PRVA01", UserRole.TECHNICIAN);
        UserAccount providerBOwner = createUser("Responsável B", "PRVB01", UserRole.TECHNICIAN);

        providerA = createProvider("Prestador A", "prestador-a", providerAOwner);
        providerB = createProvider("Prestador B", "prestador-b", providerBOwner);
        UserAccount mechanic = createUser("Mecânico A", "MECA01", UserRole.OPERATOR);
        mechanicId = mechanic.getId();
        membershipRepository.save(new OrganizationMembership(providerA, mechanic, OrganizationRole.MECHANIC, true, Instant.now()));
    }

    @Test
    void shouldKeepExternalRequestsPrivateAndMirrorExecutionToWorkOrder() throws Exception {
        String companyToken = login("CMP001");
        String providerAToken = login("PRVA01");
        String providerBToken = login("PRVB01");
        String companyBToken = login("CMP002");
        String mechanicToken = login("MECA01");

        mockMvc.perform(post("/api/provider-network/relationships")
                        .header("Authorization", bearer(companyToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"providerOrganizationId\":\"" + providerA.getId() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        UUID machineId = createMachine(companyToken, "PR-001");
        UUID otherMachineId = createMachine(companyBToken, "PR-B01");
        mockMvc.perform(get("/api/machines").header("Authorization", bearer(companyToken)))
                .andExpect(status().isOk()).andExpect(content().string(not(containsString(otherMachineId.toString()))));
        String requestResponse = mockMvc.perform(post("/api/service-requests")
                        .header("Authorization", bearer(companyToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "machineId":"%s",
                                  "title":"Falha no acionamento",
                                  "description":"Motor não parte após comando",
                                  "channel":"EXTERNAL",
                                  "priority":"HIGH",
                                  "providerOrganizationId":"%s"
                                }
                                """.formatted(machineId, providerA.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andReturn().getResponse().getContentAsString();
        UUID requestId = UUID.fromString(objectMapper.readTree(requestResponse).get("id").asText());
        mockMvc.perform(get("/api/service-requests/{id}", requestId).header("Authorization", bearer(companyBToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/api/service-requests/{id}/approve", requestId).header("Authorization", bearer(companyBToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/service-requests")
                        .header("Authorization", bearer(providerAToken)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Falha no acionamento")));

        mockMvc.perform(get("/api/service-requests")
                        .header("Authorization", bearer(providerBToken)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Falha no acionamento"))));

        mockMvc.perform(get("/api/service-requests/{id}", requestId)
                        .header("Authorization", bearer(providerBToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/service-requests/{id}/accept", requestId)
                        .header("Authorization", bearer(providerBToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());

        String accepted = mockMvc.perform(patch("/api/service-requests/{id}/accept", requestId)
                        .header("Authorization", bearer(providerAToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eta\":\"2026-09-27T21:00:00Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.workOrderId").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        UUID workOrderId = UUID.fromString(objectMapper.readTree(accepted).get("workOrderId").asText());

        mockMvc.perform(patch("/api/service-requests/{id}/assign", requestId)
                        .header("Authorization", bearer(providerAToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"technicianId\":\"" + mechanicId + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.assignedTechnicianId").value(mechanicId.toString()));

        mockMvc.perform(patch("/api/service-requests/{id}/en-route", requestId)
                        .header("Authorization", bearer(providerAToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EN_ROUTE"));

        mockMvc.perform(patch("/api/service-requests/{id}/start", requestId)
                        .header("Authorization", bearer(mechanicToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(patch("/api/service-requests/{id}/complete", requestId)
                        .header("Authorization", bearer(mechanicToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceNotes\":\"Contator substituído\",\"partsUsed\":\"Contator 24V\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/work-orders")
                        .header("Authorization", bearer(companyToken)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(workOrderId.toString())))
                .andExpect(content().string(containsString("Falha no acionamento")));

        mockMvc.perform(patch("/api/service-requests/{id}/approve", requestId)
                        .header("Authorization", bearer(companyToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    private UUID createMachine(String token, String assetCode) throws Exception {
        String plantResponse = mockMvc.perform(post("/api/admin/structure/plants")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Planta Principal\",\"code\":\"P01\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID plantId = UUID.fromString(objectMapper.readTree(plantResponse).get("id").asText());

        String sectorResponse = mockMvc.perform(post("/api/admin/structure/sectors")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plantId\":\"" + plantId + "\",\"name\":\"Montagem\",\"code\":\"S01\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID sectorId = UUID.fromString(objectMapper.readTree(sectorResponse).get("id").asText());

        String lineResponse = mockMvc.perform(post("/api/admin/structure/lines")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectorId\":\"" + sectorId + "\",\"name\":\"Linha 1\",\"code\":\"L01\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID lineId = UUID.fromString(objectMapper.readTree(lineResponse).get("id").asText());

        String machineResponse = mockMvc.perform(post("/api/machines")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productionLineId":"%s",
                                  "name":"Prensa 01",
                                  "assetCode":"%s",
                                  "status":"RUNNING"
                                }
                                """.formatted(lineId, assetCode)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(machineResponse).get("id").asText());
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

    private UserAccount createUser(String name, String registration, UserRole role) {
        Instant now = Instant.now();
        return userRepository.save(new UserAccount(
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

    private Organization createProvider(String name, String slug, UserAccount owner) {
        Organization provider = organizationRepository.save(new Organization(
                UUID.randomUUID(),
                name,
                slug,
                OrganizationType.SERVICE_PROVIDER,
                true,
                Instant.now()
        ));
        membershipRepository.save(new OrganizationMembership(provider, owner, OrganizationRole.OWNER, true, Instant.now()));
        return provider;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
