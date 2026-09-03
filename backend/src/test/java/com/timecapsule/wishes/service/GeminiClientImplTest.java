package com.timecapsule.wishes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timecapsule.wishes.enums.WishLanguage;
import com.timecapsule.wishes.exception.BusinessException;
import com.timecapsule.wishes.service.impl.GeminiClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiClientImplTest {

    private MockRestServiceServer mockServer;
    private GeminiClientImpl geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String apiKey = "test-gemini-key";
    private final String model = "gemini-2.0-flash";
    private final String baseUrl = "https://generativelanguage.googleapis.com";

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();

        geminiClient = new GeminiClientImpl(restClient, objectMapper, apiKey, model, baseUrl);
    }

    @Test
    @DisplayName("Should parse Gemini response correctly and return wish text")
    void testGenerateWish_Success() {
        String expectedUrl = String.format("%s/v1beta/models/%s:generateContent?key=%s", baseUrl, model, apiKey);

        String mockResponseBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "Chúc mừng sinh nhật! Chúc bạn tuổi mới luôn rực rỡ và thành công hơn nữa!"
                          }
                        ],
                        "role": "model"
                      },
                      "finishReason": "STOP"
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(mockResponseBody, MediaType.APPLICATION_JSON));

        String wish = geminiClient.generateWish("Birthday wish", List.of("Got promoted"), WishLanguage.VI);

        assertNotNull(wish);
        assertTrue(wish.contains("Chúc mừng sinh nhật"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw BusinessException when Gemini returns empty candidates")
    void testGenerateWish_EmptyCandidates_ThrowsException() {
        String expectedUrl = String.format("%s/v1beta/models/%s:generateContent?key=%s", baseUrl, model, apiKey);

        String mockResponseBody = "{\"candidates\": []}";

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(mockResponseBody, MediaType.APPLICATION_JSON));

        assertThrows(BusinessException.class, () ->
                geminiClient.generateWish("Test", List.of(), WishLanguage.EN));
        mockServer.verify();
    }

    @Test
    @DisplayName("Should propagate error when Gemini returns 500 error")
    void testGenerateWish_ServerError() {
        String expectedUrl = String.format("%s/v1beta/models/%s:generateContent?key=%s", baseUrl, model, apiKey);

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(Exception.class, () ->
                geminiClient.generateWish("Test", List.of(), WishLanguage.EN));
        mockServer.verify();
    }
}
