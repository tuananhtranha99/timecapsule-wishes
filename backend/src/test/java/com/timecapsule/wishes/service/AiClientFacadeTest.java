package com.timecapsule.wishes.service;

import com.timecapsule.wishes.enums.WishLanguage;
import com.timecapsule.wishes.exception.BusinessException;
import com.timecapsule.wishes.service.impl.AiClientFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiClientFacadeTest {

    @Mock
    private AiClient geminiClient;

    @Mock
    private AiClient groqClient;

    private AiClientFacade aiClientFacade;

    private final String prompt = "Birthday wish for best friend";
    private final List<String> milestones = List.of("Passed visa interview", "Bought new car");
    private final WishLanguage language = WishLanguage.VI;

    @BeforeEach
    void setUp() {
        aiClientFacade = new AiClientFacade(geminiClient, groqClient);
    }

    @Test
    @DisplayName("Should return wish from Gemini when Gemini succeeds without invoking Groq")
    void testGenerateWish_GeminiSuccess() {
        when(geminiClient.generateWish(prompt, milestones, language))
                .thenReturn("Chúc mừng sinh nhật! Một năm qua thật tuyệt vời với việc đỗ visa và mua xe mới!");

        String result = aiClientFacade.generateWish(prompt, milestones, language);

        assertNotNull(result);
        assertTrue(result.contains("Chúc mừng sinh nhật"));
        verify(geminiClient, times(1)).generateWish(prompt, milestones, language);
        verify(groqClient, never()).generateWish(any(), any(), any());
    }

    @Test
    @DisplayName("Should fallback to Groq when Gemini returns 429 Too Many Requests")
    void testGenerateWish_GeminiRateLimited_FallsBackToGroq() {
        when(geminiClient.generateWish(prompt, milestones, language))
                .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded"));
        when(groqClient.generateWish(prompt, milestones, language))
                .thenReturn("Lời chúc từ Groq Llama fallback: Chúc mừng tuổi mới!");

        String result = aiClientFacade.generateWish(prompt, milestones, language);

        assertNotNull(result);
        assertTrue(result.contains("Groq Llama"));
        verify(geminiClient, times(1)).generateWish(prompt, milestones, language);
        verify(groqClient, times(1)).generateWish(prompt, milestones, language);
    }

    @Test
    @DisplayName("Should fallback to Groq when Gemini returns 503 Service Unavailable")
    void testGenerateWish_Gemini503_FallsBackToGroq() {
        when(geminiClient.generateWish(prompt, milestones, language))
                .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE, "Gemini overloaded"));
        when(groqClient.generateWish(prompt, milestones, language))
                .thenReturn("Lời chúc từ Groq khi Gemini 503");

        String result = aiClientFacade.generateWish(prompt, milestones, language);

        assertNotNull(result);
        assertEquals("Lời chúc từ Groq khi Gemini 503", result);
        verify(geminiClient).generateWish(prompt, milestones, language);
        verify(groqClient).generateWish(prompt, milestones, language);
    }

    @Test
    @DisplayName("Should fallback to Groq when Gemini times out (ResourceAccessException)")
    void testGenerateWish_GeminiTimeout_FallsBackToGroq() {
        when(geminiClient.generateWish(prompt, milestones, language))
                .thenThrow(new ResourceAccessException("Connection timed out"));
        when(groqClient.generateWish(prompt, milestones, language))
                .thenReturn("Lời chúc từ Groq sau khi timeout");

        String result = aiClientFacade.generateWish(prompt, milestones, language);

        assertNotNull(result);
        assertEquals("Lời chúc từ Groq sau khi timeout", result);
        verify(geminiClient).generateWish(prompt, milestones, language);
        verify(groqClient).generateWish(prompt, milestones, language);
    }

    @Test
    @DisplayName("Should throw BusinessException with 503 when both Gemini and Groq fail")
    void testGenerateWish_BothFail_ThrowsServiceUnavailable() {
        when(geminiClient.generateWish(prompt, milestones, language))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Gemini failed"));
        when(groqClient.generateWish(prompt, milestones, language))
                .thenThrow(new HttpServerErrorException(HttpStatus.BAD_GATEWAY, "Groq failed"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> aiClientFacade.generateWish(prompt, milestones, language));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        assertTrue(exception.getMessage().contains("AI wish generation service is currently unavailable"));
        verify(geminiClient).generateWish(prompt, milestones, language);
        verify(groqClient).generateWish(prompt, milestones, language);
    }
}
