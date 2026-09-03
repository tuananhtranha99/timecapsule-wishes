package com.timecapsule.wishes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timecapsule.wishes.dto.request.CreateRecipientRequest;
import com.timecapsule.wishes.dto.request.RegisterRequest;
import com.timecapsule.wishes.dto.request.UpdateRecipientRequest;
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
class RecipientControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecipientRepository recipientRepository;

    private String user1Token;
    private String user2Token;

    @BeforeEach
    void setUp() throws Exception {
        recipientRepository.deleteAll();
        userRepository.deleteAll();

        user1Token = registerAndGetToken("user1@example.com", "pass12345", "User One");
        user2Token = registerAndGetToken("user2@example.com", "pass12345", "User Two");
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
    @DisplayName("Should successfully create a recipient when authenticated")
    void testCreateRecipient_Success() throws Exception {
        CreateRecipientRequest request = new CreateRecipientRequest(
                "Charlie Brown",
                LocalDate.of(2000, 1, 15),
                "Friend",
                "Likes baseball"
        );

        mockMvc.perform(post("/api/v1/recipients")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.name", is("Charlie Brown")))
                .andExpect(jsonPath("$.data.relationship", is("Friend")))
                .andExpect(jsonPath("$.data.birthday", is("2000-01-15")))
                .andExpect(jsonPath("$.data.notes", is("Likes baseball")));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when validation fails on create")
    void testCreateRecipient_ValidationFailure() throws Exception {
        CreateRecipientRequest request = new CreateRecipientRequest(
                "", // Blank name violates @NotBlank
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/recipients")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.validationErrors.name", notNullValue()));
    }

    @Test
    @DisplayName("Should isolate recipients by authenticated user")
    void testGetRecipients_UserIsolation() throws Exception {
        CreateRecipientRequest req1 = new CreateRecipientRequest("User1 Recipient", null, "Colleague", null);
        mockMvc.perform(post("/api/v1/recipients")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        // User 2 lists recipients -> should be empty
        mockMvc.perform(get("/api/v1/recipients")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(0)));

        // User 1 lists recipients -> should have 1 item
        mockMvc.perform(get("/api/v1/recipients")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("User1 Recipient")));
    }

    @Test
    @DisplayName("Should update recipient and prevent cross-user updates")
    void testUpdateRecipient_SuccessAndCrossUserProtection() throws Exception {
        CreateRecipientRequest createReq = new CreateRecipientRequest("Original Name", null, "Peer", null);
        MvcResult createResult = mockMvc.perform(post("/api/v1/recipients")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String recipientId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data")
                .get("id")
                .asText();

        // User 2 attempts to update User 1's recipient -> 404 Not Found
        UpdateRecipientRequest updateReq = new UpdateRecipientRequest("Hacked Name", null, "None", null);
        mockMvc.perform(put("/api/v1/recipients/" + recipientId)
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        // User 1 successfully updates
        UpdateRecipientRequest validUpdate = new UpdateRecipientRequest("Updated Name", null, "Mentor", "Great mentor");
        mockMvc.perform(put("/api/v1/recipients/" + recipientId)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Updated Name")))
                .andExpect(jsonPath("$.data.relationship", is("Mentor")))
                .andExpect(jsonPath("$.data.notes", is("Great mentor")));
    }

    @Test
    @DisplayName("Should delete recipient and prevent subsequent access")
    void testDeleteRecipient() throws Exception {
        CreateRecipientRequest createReq = new CreateRecipientRequest("To Delete", null, null, null);
        MvcResult createResult = mockMvc.perform(post("/api/v1/recipients")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String recipientId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data")
                .get("id")
                .asText();

        // Delete recipient
        mockMvc.perform(delete("/api/v1/recipients/" + recipientId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Verify it is gone
        mockMvc.perform(get("/api/v1/recipients/" + recipientId)
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when unauthenticated")
    void testUnauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/recipients"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)));
    }
}
