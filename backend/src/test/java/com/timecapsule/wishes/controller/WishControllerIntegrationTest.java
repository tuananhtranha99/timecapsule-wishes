package com.timecapsule.wishes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timecapsule.wishes.dto.request.CreateMilestoneRequest;
import com.timecapsule.wishes.dto.request.CreateRecipientRequest;
import com.timecapsule.wishes.dto.request.EditWishRequest;
import com.timecapsule.wishes.dto.request.GenerateWishRequest;
import com.timecapsule.wishes.dto.request.RegisterRequest;
import com.timecapsule.wishes.enums.MilestoneCategory;
import com.timecapsule.wishes.enums.OccasionType;
import com.timecapsule.wishes.enums.WishLanguage;
import com.timecapsule.wishes.repository.GeneratedWishRepository;
import com.timecapsule.wishes.repository.MilestoneRepository;
import com.timecapsule.wishes.repository.RecipientRepository;
import com.timecapsule.wishes.repository.UserRepository;
import com.timecapsule.wishes.service.AiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WishControllerIntegrationTest {

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

    @Autowired
    private GeneratedWishRepository generatedWishRepository;

    @MockBean
    private AiClient aiClient;

    private String user1Token;
    private String user2Token;
    private String user1RecipientId;
    private String user1MilestoneId;

    @BeforeEach
    void setUp() throws Exception {
        generatedWishRepository.deleteAll();
        milestoneRepository.deleteAll();
        recipientRepository.deleteAll();
        userRepository.deleteAll();

        when(aiClient.generateWish(anyString(), anyList(), any()))
                .thenReturn("Chúc mừng sinh nhật! Một năm qua bạn đã làm được nhiều điều thật phi thường!");

        user1Token = registerAndGetToken("user1@example.com", "pass12345", "User One");
        user2Token = registerAndGetToken("user2@example.com", "pass12345", "User Two");

        // Create recipient for user 1
        CreateRecipientRequest recReq = new CreateRecipientRequest("Friend Bob", LocalDate.of(1998, 4, 12), "Best Friend", null);
        MvcResult recResult = mockMvc.perform(post("/api/v1/recipients")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recReq)))
                .andExpect(status().isCreated())
                .andReturn();

        user1RecipientId = objectMapper.readTree(recResult.getResponse().getContentAsString())
                .get("data").get("id").asText();

        // Create milestone for user 1's recipient
        CreateMilestoneRequest msReq = new CreateMilestoneRequest("Ran marathon", MilestoneCategory.ACHIEVEMENT, LocalDate.of(2025, 2, 1));
        MvcResult msResult = mockMvc.perform(post("/api/v1/recipients/" + user1RecipientId + "/milestones")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(msReq)))
                .andExpect(status().isCreated())
                .andReturn();

        user1MilestoneId = objectMapper.readTree(msResult.getResponse().getContentAsString())
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
                .get("data").get("accessToken").asText();
    }

    @Test
    @DisplayName("Should successfully generate wish referencing milestones and save to DB")
    void testGenerateWish_Success() throws Exception {
        GenerateWishRequest request = new GenerateWishRequest(
                UUID.fromString(user1RecipientId),
                List.of(UUID.fromString(user1MilestoneId)),
                OccasionType.BIRTHDAY,
                WishLanguage.VI,
                "Giọng điệu ấm áp và hài hước"
        );

        mockMvc.perform(post("/api/v1/wishes/generate")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.recipientId", is(user1RecipientId)))
                .andExpect(jsonPath("$.data.version", is(1)))
                .andExpect(jsonPath("$.data.generatedText", notNullValue()))
                .andExpect(jsonPath("$.data.milestoneIds", hasSize(1)));
    }

    @Test
    @DisplayName("Should generate wish with zero milestones provided (generic fallback)")
    void testGenerateWish_ZeroMilestones() throws Exception {
        GenerateWishRequest request = new GenerateWishRequest(
                UUID.fromString(user1RecipientId),
                List.of(),
                OccasionType.BIRTHDAY,
                WishLanguage.EN,
                null
        );

        mockMvc.perform(post("/api/v1/wishes/generate")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.version", is(1)))
                .andExpect(jsonPath("$.data.milestoneIds", hasSize(0)));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when validation fails on wish generation")
    void testGenerateWish_ValidationFailure() throws Exception {
        GenerateWishRequest request = new GenerateWishRequest(
                null, // null recipientId
                null,
                null, // null occasionType
                null, // null language
                null
        );

        mockMvc.perform(post("/api/v1/wishes/generate")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Should return 404 when attempting to generate wish for another user's recipient")
    void testGenerateWish_CrossUserRecipient_Returns404() throws Exception {
        GenerateWishRequest request = new GenerateWishRequest(
                UUID.fromString(user1RecipientId),
                List.of(),
                OccasionType.BIRTHDAY,
                WishLanguage.VI,
                null
        );

        mockMvc.perform(post("/api/v1/wishes/generate")
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should save edited text and increment version counter")
    void testEditWish_Success() throws Exception {
        GenerateWishRequest genReq = new GenerateWishRequest(
                UUID.fromString(user1RecipientId),
                List.of(),
                OccasionType.BIRTHDAY,
                WishLanguage.VI,
                null
        );

        MvcResult genResult = mockMvc.perform(post("/api/v1/wishes/generate")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String wishId = objectMapper.readTree(genResult.getResponse().getContentAsString())
                .get("data").get("id").asText();

        // Edit wish
        EditWishRequest editReq = new EditWishRequest("Đây là bản chỉnh sửa cá nhân hóa của tôi!");
        mockMvc.perform(put("/api/v1/wishes/" + wishId)
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.version", is(2)))
                .andExpect(jsonPath("$.data.editedText", is("Đây là bản chỉnh sửa cá nhân hóa của tôi!")));
    }

    @Test
    @DisplayName("Should return 404 when another user tries to edit a wish")
    void testEditWish_CrossUser_Returns404() throws Exception {
        GenerateWishRequest genReq = new GenerateWishRequest(
                UUID.fromString(user1RecipientId),
                List.of(),
                OccasionType.BIRTHDAY,
                WishLanguage.VI,
                null
        );

        MvcResult genResult = mockMvc.perform(post("/api/v1/wishes/generate")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String wishId = objectMapper.readTree(genResult.getResponse().getContentAsString())
                .get("data").get("id").asText();

        EditWishRequest editReq = new EditWishRequest("Hack attempt");
        mockMvc.perform(put("/api/v1/wishes/" + wishId)
                        .header("Authorization", "Bearer " + user2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editReq)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return wish history for recipient and enforce user isolation")
    void testGetWishHistory_UserIsolation() throws Exception {
        GenerateWishRequest genReq = new GenerateWishRequest(
                UUID.fromString(user1RecipientId),
                List.of(),
                OccasionType.BIRTHDAY,
                WishLanguage.VI,
                null
        );

        mockMvc.perform(post("/api/v1/wishes/generate")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(genReq)))
                .andExpect(status().isCreated());

        // User 1 sees wish history
        mockMvc.perform(get("/api/v1/recipients/" + user1RecipientId + "/wishes")
                        .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        // User 2 tries to view User 1's recipient wishes -> 404
        mockMvc.perform(get("/api/v1/recipients/" + user1RecipientId + "/wishes")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 401 when accessing wishes unauthenticated")
    void testUnauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/recipients/" + user1RecipientId + "/wishes"))
                .andExpect(status().isUnauthorized());
    }
}
