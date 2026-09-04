package com.timecapsule.wishes.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.timecapsule.wishes.enums.WishLanguage;
import com.timecapsule.wishes.exception.BusinessException;
import com.timecapsule.wishes.service.AiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component("geminiClient")
@Slf4j
public class GeminiClientImpl implements AiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public GeminiClientImpl(
            RestClient restClient,
            ObjectMapper objectMapper,
            @Value("${app.ai.gemini.api-key:mock-gemini-key}") String apiKey,
            @Value("${app.ai.gemini.model:gemini-3.6-flash}") String model,
            @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
    }

    @Override
    public String generateWish(String prompt, List<String> milestones, WishLanguage language) {
        String fullPrompt = buildPrompt(prompt, milestones, language);

        List<String> candidateModels = new ArrayList<>();
        if (model != null && !model.isBlank()) {
            candidateModels.add(model.trim());
        }
        for (String m : List.of("gemini-3.6-flash", "gemini-2.5-flash", "gemini-1.5-flash")) {
            if (!candidateModels.contains(m)) {
                candidateModels.add(m);
            }
        }

        Exception lastException = null;
        for (String targetModel : candidateModels) {
            try {
                log.info("Attempting wish generation via Google Gemini model: {}", targetModel);
                return executeGeminiCall(targetModel, fullPrompt);
            } catch (HttpClientErrorException.NotFound notFoundEx) {
                log.warn("Gemini model '{}' not found (404). Trying next candidate...", targetModel);
                lastException = notFoundEx;
            } catch (Exception ex) {
                if (ex.getMessage() != null && ex.getMessage().contains("404")) {
                    log.warn("Gemini model '{}' returned 404: {}. Trying next candidate...", targetModel, ex.getMessage());
                    lastException = ex;
                    continue;
                }
                throw ex;
            }
        }

        throw new BusinessException("Gemini failed for all candidate models: " + (lastException != null ? lastException.getMessage() : "Unknown"), HttpStatus.BAD_GATEWAY);
    }

    private String executeGeminiCall(String targetModel, String fullPrompt) {
        ObjectNode payload = objectMapper.createObjectNode();
        ArrayNode contents = payload.putArray("contents");
        ObjectNode contentItem = contents.addObject();
        ArrayNode parts = contentItem.putArray("parts");
        parts.addObject().put("text", fullPrompt);

        ObjectNode generationConfig = payload.putObject("generationConfig");
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 1000);

        String url = String.format("%s/v1beta/models/%s:generateContent?key=%s", baseUrl, targetModel, apiKey);

        JsonNode responseNode = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        if (responseNode == null) {
            throw new BusinessException("Gemini returned null response", HttpStatus.BAD_GATEWAY);
        }

        JsonNode candidate = responseNode.path("candidates").get(0);
        if (candidate != null && candidate.has("content")) {
            JsonNode candidateParts = candidate.path("content").path("parts");
            if (candidateParts.isArray() && !candidateParts.isEmpty()) {
                String wishText = candidateParts.get(0).path("text").asText().trim();
                if (!wishText.isBlank()) {
                    log.info("Wish successfully generated via Gemini model {} (length: {})", targetModel, wishText.length());
                    return wishText;
                }
            }
        }

        throw new BusinessException("Gemini returned an empty response", HttpStatus.BAD_GATEWAY);
    }

    private String buildPrompt(String prompt, List<String> milestones, WishLanguage language) {
        StringBuilder sb = new StringBuilder();
        if (language == WishLanguage.VI) {
            sb.append("Bạn là một người bạn thân thiết, ấm áp và chân thành. ");
            sb.append("Hãy viết một lời chúc thật cảm động, tự nhiên và mang tính cá nhân hóa cao ");
            sb.append("dựa trên các cột mốc (milestones) đáng nhớ sau đây của người nhận trong năm qua.\n\n");
        } else {
            sb.append("You are a warm, thoughtful, and genuine friend. ");
            sb.append("Write a touching, natural, and deeply personalized wish message ");
            sb.append("based on the memorable milestones the recipient experienced over the past year.\n\n");
        }

        if (prompt != null && !prompt.isBlank()) {
            sb.append("Thông tin thêm (Additional Context): ").append(prompt).append("\n\n");
        }

        if (milestones != null && !milestones.isEmpty()) {
            sb.append("Các cột mốc đạt được (Milestones):\n");
            for (String milestone : milestones) {
                sb.append("- ").append(milestone).append("\n");
            }
        } else {
            sb.append("Lưu ý: Không có cột mốc cụ thể nào được ghi lại. Hãy viết một lời chúc chân thành, ấm áp và ý nghĩa chung.\n");
        }

        if (language == WishLanguage.VI) {
            sb.append("\nYêu cầu về lời chúc:\n");
            sb.append("1. Ngôn ngữ: Tiếng Việt tự nhiên, ấm áp, tình cảm.\n");
            sb.append("2. Lồng ghép các cột mốc một cách tinh tế và khéo léo, không liệt kê máy móc.\n");
            sb.append("3. Chỉ trả về nội dung lời chúc, không thêm lời mở đầu chào hỏi của AI hay giải thích gì thêm.");
        } else {
            sb.append("\nWish requirements:\n");
            sb.append("1. Language: Natural, warm, and heartfelt English.\n");
            sb.append("2. Weave the milestones seamlessly and meaningfully, do not mechanically list them.\n");
            sb.append("3. Return ONLY the wish text itself, without introductory AI greetings or explanations.");
        }

        return sb.toString();
    }
}
