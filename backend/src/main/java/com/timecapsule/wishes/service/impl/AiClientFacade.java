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

    public AiClientFacade(
            @Qualifier("geminiClient") AiClient geminiClient,
            @Qualifier("groqClient") AiClient groqClient
    ) {
        this.geminiClient = geminiClient;
        this.groqClient = groqClient;
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
                log.error("Secondary AI provider (Groq) also failed: '{}'. No more fallbacks.",
                        fallbackEx.getMessage(), fallbackEx);
                throw new BusinessException(
                        "AI wish generation service is currently unavailable. Please try again later.",
                        HttpStatus.SERVICE_UNAVAILABLE
                );
            }
        }
    }
}
