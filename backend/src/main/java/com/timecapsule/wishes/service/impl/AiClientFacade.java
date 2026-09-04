package com.timecapsule.wishes.service.impl;

import com.timecapsule.wishes.enums.WishLanguage;
import com.timecapsule.wishes.exception.BusinessException;
import com.timecapsule.wishes.service.AiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Primary
@Slf4j
public class AiClientFacade implements AiClient {

    private final AiClient geminiClient;
    private final AiClient groqClient;
    private final AiClient smartFallbackClient;

    public AiClientFacade(
            @Qualifier("geminiClient") AiClient geminiClient,
            @Qualifier("groqClient") AiClient groqClient,
            @Qualifier("smartFallbackClient") AiClient smartFallbackClient
    ) {
        this.geminiClient = geminiClient;
        this.groqClient = groqClient;
        this.smartFallbackClient = smartFallbackClient;
    }

    @Override
    public String generateWish(String prompt, List<String> milestones, WishLanguage language) {
        try {
            log.info("Attempting wish generation via primary provider (Gemini)...");
            return geminiClient.generateWish(prompt, milestones, language);
        } catch (Exception e) {
            log.warn("Primary AI provider (Gemini) failed: '{}'. Falling back to secondary provider (Groq)...",
                    e.getMessage());
            try {
                return groqClient.generateWish(prompt, milestones, language);
            } catch (Exception fallbackEx) {
                log.warn("Secondary AI provider (Groq) also failed: '{}'. Falling back to Smart AI Synthesizer...",
                        fallbackEx.getMessage());
                try {
                    return smartFallbackClient.generateWish(prompt, milestones, language);
                } catch (Exception fatalEx) {
                    log.error("All AI providers failed including smart fallback: '{}'", fatalEx.getMessage(), fatalEx);
                    throw new BusinessException(
                            "AI wish generation service is currently unavailable. Please try again later.",
                            HttpStatus.SERVICE_UNAVAILABLE
                    );
                }
            }
        }
    }
}
