package com.timecapsule.wishes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timecapsule.wishes.dto.request.CreateMilestoneRequest;
import com.timecapsule.wishes.dto.request.CreateRecipientRequest;
import com.timecapsule.wishes.dto.request.RegisterRequest;
import com.timecapsule.wishes.dto.request.UpdateMilestoneRequest;
import com.timecapsule.wishes.enums.MilestoneCategory;
import com.timecapsule.wishes.repository.MilestoneRepository;
import com.timecapsule.wishes.repository.RecipientRepository;
import com.timecapsule.wishes.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MilestoneControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecipientRepository recipientRepository;

    @Autowired
    private MilestoneRepository milestoneRepository;

    private String user1Token;
    private String user2Token;
    private String user1RecipientId;

    @BeforeEach
    void setUp() throws Exception {
        milestoneRepository.deleteAll();
        recipientRepository.deleteAll();
        userRepository.deleteAll();

        user1Token = registerAndGetToken("user1@example.com", "pass12345", "User One");
        user2Token = registerAndGetToken("user2@example.com", "pass12345", "User Two");

        // Create a recipient for User 1
        CreateRecipientRequest recipientReq = new CreateRecipientRequest("Mom", LocalDate.of(1970, 8, 15), "Mother", null);
        MvcResult recResult = mockMvc.perform(post("/api/v1/recipients")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recipientReq)))
                .andExpect(status().isCreated())
                .andReturn();

        user1RecipientId = objectMapper.readTree(recResult.getResponse().getContentAsString())
                .get("data").get("id").asText();
    }

    private String registerAndGetToken(String email, String password, String displayName) throws Exception {
        RegisterRequest req = new RegisterRequest(email, password, displayName);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data")
                .get("accessToken")
                .asText();
    }

    @Test
    @DisplayName("Should successfully create milestone with backdated occurred_at date")
    void testCreateMilestone_Success() throws Exception {
        CreateMilestoneRequest request = new CreateMilestoneRequest(
                "Completed her master thesis",
                MilestoneCategory.ACHIEVEMENT,
                LocalDate.of(2024, 6, 20) // backdated
        );

        mockMvc.perform(post("/api/v1/recipients/" + user1RecipientId + "/milestones")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.recipientId", is(user1RecipientId)))
                .andExpect(jsonPath("$.data.description", is("Completed her master thesis")))
                .andExpect(jsonPath("$.data.category", is("ACHIEVEMENT")))
                .andExpect(jsonPath("$.data.occurredAt", is("2024-06-20")));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when milestone validation fails")
    void testCreateMilestone_ValidationFailure() throws Exception {
        CreateMilestoneRequest request = new CreateMilestoneRequest(
                "", // Blank description
                null,
                null
        );

        mockMvc.perform(post("/api/v1/recipients/" + user1RecipientId + "/milestones")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.validationErrors.description", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.category", notNullValue()))
                .andExpect(jsonPath("$.validationErrors.occurredAt", notNullValue()));
    }

    @Test
    @DisplayName("Should return 404 when user attempts to add milestone to another user's recipient")
    void testCreateMilestone_CrossUserForbidden() throws Exception {
        CreateMilestoneRequest request = new CreateMilestoneRequest(
                "Ran 10k race",
                MilestoneCategory.HEALTH,
                LocalDate.of(2025, 1, 10)
        );

        // User 2 tries to log a milestone on User 1's recipient
        mockMvc.perform(post("/api/v1/recipients/" + user1RecipientId + "/milestones")
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should isolate milestones by recipient and owner user")
    void testGetMilestones_UserIsolation() throws Exception {
        CreateMilestoneRequest request = new CreateMilestoneRequest(
                "Traveled to Japan",
                MilestoneCategory.TRAVEL,
                LocalDate.of(2025, 4, 1)
        );

        mockMvc.perform(post("/api/v1/recipients/" + user1RecipientId + "/milestones")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // User 1 lists milestones -> 1 item
        mockMvc.perform(get("/api/v1/recipients/" + user1RecipientId + "/milestones")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].description", is("Traveled to Japan")));

        // User 2 tries to list milestones of User 1's recipient -> 404
        mockMvc.perform(get("/api/v1/recipients/" + user1RecipientId + "/milestones")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update milestone and protect against cross-user updates")
    void testUpdateMilestone_SuccessAndCrossUserProtection() throws Exception {
        CreateMilestoneRequest createReq = new CreateMilestoneRequest(
                "Initial description",
                MilestoneCategory.CAREER,
                LocalDate.of(2025, 2, 1)
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/recipients/" + user1RecipientId + "/milestones")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String milestoneId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asText();

        // User 2 attempts to update -> 404
        UpdateMilestoneRequest updateReq = new UpdateMilestoneRequest(
                "Hacked description",
                MilestoneCategory.OTHER,
                LocalDate.of(2025, 2, 1)
        );
        mockMvc.perform(put("/api/v1/milestones/" + milestoneId)
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        // User 1 successfully updates
        UpdateMilestoneRequest validUpdate = new UpdateMilestoneRequest(
                "Promoted to Senior Engineer",
                MilestoneCategory.CAREER,
                LocalDate.of(2025, 2, 1)
        );
        mockMvc.perform(put("/api/v1/milestones/" + milestoneId)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description", is("Promoted to Senior Engineer")));
    }

    @Test
    @DisplayName("Should delete milestone and prevent subsequent access")
    void testDeleteMilestone() throws Exception {
        CreateMilestoneRequest createReq = new CreateMilestoneRequest(
                "Temporary milestone",
                MilestoneCategory.OTHER,
                LocalDate.of(2025, 1, 1)
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/recipients/" + user1RecipientId + "/milestones")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String milestoneId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asText();

        // Delete
        mockMvc.perform(delete("/api/v1/milestones/" + milestoneId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Verify gone
        mockMvc.perform(get("/api/v1/milestones/" + milestoneId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 401 when accessing milestones unauthenticated")
    void testUnauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/recipients/" + user1RecipientId + "/milestones"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
