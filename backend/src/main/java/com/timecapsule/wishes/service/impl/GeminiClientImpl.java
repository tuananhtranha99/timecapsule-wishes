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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
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
            } catch (RuntimeException ex) {
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
        generationConfig.put("maxOutputTokens", 450);

        String url = String.format("%s/v1beta/models/%s:generateContent?key=%s", baseUrl, targetModel, apiKey);

        // Use exchange to directly read raw byte stream, completely immune to any content-type (application/octet-stream, text/plain, etc.)
        byte[] bytes = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.ALL)
                .body(payload)
                .exchange((request, response) -> {
                    byte[] bodyBytes = response.getBody().readAllBytes();
                    if (response.getStatusCode().isError()) {
                        String errorText = new String(bodyBytes, StandardCharsets.UTF_8);
                        log.warn("Gemini model '{}' returned HTTP {}: {}", targetModel, response.getStatusCode(), errorText);
                        if (response.getStatusCode().value() == 404 || errorText.contains("404") || errorText.contains("not found") || errorText.contains("no longer available")) {
                            throw new HttpClientErrorException.NotFound("Gemini model unavailable: " + errorText, response.getHeaders(), bodyBytes, StandardCharsets.UTF_8);
                        }
                        throw new BusinessException("Gemini HTTP " + response.getStatusCode() + ": " + errorText, HttpStatus.BAD_GATEWAY);
                    }
                    return bodyBytes;
                });

        if (bytes == null || bytes.length == 0) {
            throw new BusinessException("Gemini returned empty response body", HttpStatus.BAD_GATEWAY);
        }

        String responseBody = new String(bytes, StandardCharsets.UTF_8);

        JsonNode responseNode;
        try {
            responseNode = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            throw new BusinessException("Failed to parse Gemini response: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
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
            sb.append("Bạn là người viết lời chúc chân thành, gần gũi và sâu sắc. ");
            sb.append("Hãy viết một lời chúc ngắn gọn, súc tích (khoảng 2 đến 3 đoạn văn ngắn, tuyệt đối không viết dài dòng lê thê) ");
            sb.append("dựa trên các thông tin và cột mốc đáng nhớ sau đây.\n\n");
        } else {
            sb.append("You are a thoughtful, warm, and authentic wish writer. ");
            sb.append("Write a concise, heartfelt wish (around 2 to 3 short paragraphs, avoid being overly verbose) ");
            sb.append("based on the memorable milestones below.\n\n");
        }

        if (prompt != null && !prompt.isBlank()) {
            sb.append("Thông tin bối cảnh (Context): ").append(prompt).append("\n\n");
        }

        if (milestones != null && !milestones.isEmpty()) {
            sb.append("Các cột mốc đạt được (Milestones - hãy đưa đầy đủ vào lời chúc):\n");
            for (String milestone : milestones) {
                sb.append("- ").append(milestone).append("\n");
            }
        } else {
            sb.append("Lưu ý: Không có cột mốc cụ thể nào được ghi lại. Hãy viết một lời chúc chân thành, ấm áp và ý nghĩa chung.\n");
        }

        if (language == WishLanguage.VI) {
            sb.append("\nYêu cầu bắt buộc khi tạo lời chúc:\n");
            sb.append("1. Độ dài: Ngắn gọn, cô đọng (chỉ từ 2 đến 3 đoạn văn ngắn), tránh lan man dài dòng.\n");
            sb.append("2. Lồng ghép cột mốc: Phải nhắc đến đầy đủ các cột mốc trên nhưng không liệt kê máy móc, không lặp từ (tránh lặp lại nhiều lần các cụm từ như 'chúc mừng bạn đã...', 'thật tự hào khi bạn...'). Hãy xâu chuỗi chúng một cách tự nhiên.\n");
            sb.append("3. Giọng văn: Giản dị, chân thành, tự nhiên như lời nhắn gửi giữa người thân/bạn bè ngoài đời. Tuyệt đối không dùng từ ngữ sáo rỗng, đao to búa lớn hay văn mẫu hoa mỹ quá đà.\n");
            sb.append("4. Cách xưng hô: Tuân thủ tuyệt đối thông tin xưng hô (Xưng và Hô) nếu đã được cung cấp trong phần bối cảnh.\n");
            sb.append("5. Chỉ trả về nội dung lời chúc hoàn chỉnh, không thêm lời chào mở đầu của AI hay bất kỳ lời giải thích nào.");
        } else {
            sb.append("\nStrict requirements:\n");
            sb.append("1. Length: Concise and focused (2-3 short paragraphs max), no unnecessary filler.\n");
            sb.append("2. Milestones: Weave all provided milestones seamlessly, without repetitive sentence structures.\n");
            sb.append("3. Tone: Down-to-earth, sincere, and natural. Avoid pompous or clichéd corporate jargon.\n");
            sb.append("4. Addressing: Strictly respect any specified pronouns and tone.\n");
            sb.append("5. Return ONLY the wish text itself, without introductory AI greetings or explanations.");
        }

        return sb.toString();
    }
}
