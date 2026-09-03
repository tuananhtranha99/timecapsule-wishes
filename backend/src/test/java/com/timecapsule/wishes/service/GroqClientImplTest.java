package com.timecapsule.wishes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timecapsule.wishes.enums.WishLanguage;
import com.timecapsule.wishes.exception.BusinessException;
import com.timecapsule.wishes.service.impl.GroqClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GroqClientImplTest {

    private MockRestServiceServer mockServer;
    private GroqClientImpl groqClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String apiKey = "test-groq-key";
    private final String model = "llama-3.1-70b-versatile";
    private final String baseUrl = "https://api.groq.com";

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();

        groqClient = new GroqClientImpl(restClient, objectMapper, apiKey, model, baseUrl);
    }

    @Test
    @DisplayName("Should parse Groq OpenAI-compatible response correctly and return wish text")
    void testGenerateWish_Success() {
        String expectedUrl = String.format("%s/openai/v1/chat/completions", baseUrl);

        String mockResponseBody = """
                {
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": "Happy birthday! It's been an incredible year watching you achieve your goals!"
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey))
                .andRespond(withSuccess(mockResponseBody, MediaType.APPLICATION_JSON));

        String wish = groqClient.generateWish("Birthday wish", List.of("Achieved goals"), WishLanguage.EN);

        assertNotNull(wish);
        assertTrue(wish.contains("Happy birthday"));
        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw BusinessException when Groq returns empty choices")
    void testGenerateWish_EmptyChoices_ThrowsException() {
        String expectedUrl = String.format("%s/openai/v1/chat/completions", baseUrl);

        String mockResponseBody = "{\"choices\": []}";

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(mockResponseBody, MediaType.APPLICATION_JSON));

        assertThrows(BusinessException.class, () ->
                groqClient.generateWish("Test", List.of(), WishLanguage.EN));
        mockServer.verify();
    }

    @Test
    @DisplayName("Should propagate error when Groq returns 500 server error")
    void testGenerateWish_ServerError() {
        String expectedUrl = String.format("%s/openai/v1/chat/completions", baseUrl);

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(Exception.class, () ->
                groqClient.generateWish("Test", List.of(), WishLanguage.EN));
        mockServer.verify();
    }
}
