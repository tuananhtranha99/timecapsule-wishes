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
            } catch (BusinessException be) {
                if (be.getStatus() == HttpStatus.NOT_FOUND) {
                    log.warn("Gemini model '{}' not found (404). Trying next candidate...", targetModel);
                    lastException = be;
                    continue;
                }
                throw be;
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
        generationConfig.put("maxOutputTokens", 2048);

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
                            throw new BusinessException("Gemini model unavailable: " + errorText, HttpStatus.NOT_FOUND);
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
            String finishReason = candidate.path("finishReason").asText("");
            if ("MAX_TOKENS".equalsIgnoreCase(finishReason)) {
                log.warn("Gemini model '{}' hit MAX_TOKENS finishReason!", targetModel);
            }
            JsonNode candidateParts = candidate.path("content").path("parts");
            if (candidateParts.isArray() && !candidateParts.isEmpty()) {
                StringBuilder wishBuilder = new StringBuilder();
                for (JsonNode partNode : candidateParts) {
                    if (partNode.has("thought") && partNode.get("thought").asBoolean()) {
                        continue; // Skip thinking tokens in Gemini reasoning models
                    }
                    String partText = partNode.path("text").asText("");
                    if (!partText.isEmpty()) {
                        wishBuilder.append(partText);
                    }
                }
                String wishText = wishBuilder.toString().trim();
                if (!wishText.isBlank()) {
                    log.info("Wish successfully generated via Gemini model {} (length: {}, finishReason: {})",
                            targetModel, wishText.length(), finishReason);
                    return wishText;
                }
            }
        }

        throw new BusinessException("Gemini returned an empty response", HttpStatus.BAD_GATEWAY);
    }

    private String buildPrompt(String prompt, List<String> milestones, WishLanguage language) {
        StringBuilder sb = new StringBuilder();
        if (language == WishLanguage.VI) {
            sb.append("Bạn là người viết lời chúc chân thành, sâu sắc và tinh tế.\n");
            sb.append("Hãy tạo một lời chúc hoàn chỉnh, ý nghĩa, có độ dài vừa vặn (khoảng 150 đến 250 từ, chia làm 2 đến 3 đoạn văn ngắn gọn, súc tích). ");
            sb.append("Tuyệt đối không viết lan man dài dòng, nhưng bắt buộc phải viết trọn vẹn từ mở đầu, thân bài điểm lại các cột mốc đến lời chúc kết thúc, tuyệt đối không được bỏ lửng hay ngắt cụt câu giữa chừng.\n\n");
        } else {
            sb.append("You are a thoughtful, authentic, and caring wish writer.\n");
            sb.append("Create a complete, meaningful, and well-proportioned wish (around 150 to 250 words, formatted in 2 to 3 concise paragraphs). ");
            sb.append("Avoid unnecessary fluff, but ensure the wish is complete, heartfelt, and never abruptly cut off.\n\n");
        }

        if (prompt != null && !prompt.isBlank()) {
            sb.append(language == WishLanguage.VI ? "Thông tin bối cảnh:\n" : "Context:\n")
              .append(prompt).append("\n\n");
        }

        if (milestones != null && !milestones.isEmpty()) {
            sb.append(language == WishLanguage.VI
                    ? "Các cột mốc / thành tựu trong năm qua (BẮT BUỘC nhắc đến đầy đủ tất cả các mốc này trong lời chúc):\n"
                    : "Memorable milestones achieved in the past year (MUST all be seamlessly included in the wish):\n");
            for (String milestone : milestones) {
                sb.append("- ").append(milestone).append("\n");
            }
        } else {
            sb.append(language == WishLanguage.VI
                    ? "Lưu ý: Không có cột mốc cụ thể nào được ghi lại. Hãy viết một lời chúc chân thành, ấm áp và ý nghĩa chung.\n"
                    : "Note: No specific milestones recorded. Write a warm, genuine, and meaningful general wish.\n");
        }

        if (language == WishLanguage.VI) {
            sb.append("\nQuy tắc bắt buộc khi viết lời chúc:\n");
            sb.append("1. NGÔN NGỮ: BẮT BUỘC viết 100% bằng TIẾNG VIỆT tự nhiên, đời thường. Tuyệt đối KHÔNG chêm tiếng Anh (ví dụ: ngày sinh nhật thì dùng 'Chúc mừng sinh nhật...', KHÔNG dùng 'Happy birthday').\n");
            sb.append("2. TÍNH TRỌN VẸN & ĐỘ DÀI: Viết một bức thông điệp HOÀN CHỈNH gồm lời chào/mở đầu, thân bài điểm lại các cột mốc, và lời chúc kết lại trọn vẹn (khoảng 150-250 từ, 2-3 đoạn ngắn). Lời chúc phải có đầu có đuôi, tuyệt đối không ngắt ngang cụt lủn.\n");
            sb.append("3. LỒNG GHÉP CỘT MỐC ĐẦY ĐỦ: Phải đưa đầy đủ tất cả các cột mốc đã liệt kê ở trên vào lời chúc một cách mượt mà, tự nhiên. Không liệt kê thô cứng như gạch đầu dòng báo cáo. KHÔNG lặp từ hoặc lặp cấu trúc câu (tránh lặp đi lặp lại 'chúc mừng bạn đã...', 'thật tuyệt khi...').\n");
            sb.append("4. GIỌNG ĐIỆU CHÂN THÀNH: Giản dị, ấm áp, sâu sắc như lời nhắn gửi giữa người thân, bạn bè ngoài đời thật. KHÔNG dùng văn mẫu sáo rỗng, KHÔNG đao to búa lớn hay từ ngữ hoa mỹ quá đà.\n");
            sb.append("5. XƯNG HÔ CHUẨN XÁC: Tuân thủ tuyệt đối cách xưng hô (Xưng là gì, Hô người nhận là gì) như đã được cung cấp trong thông tin bối cảnh.\n");
            sb.append("6. ĐẦU RA DUY NHẤT: Chỉ trả về duy nhất nội dung lời chúc hoàn chỉnh. Tuyệt đối không thêm lời chào mở đầu của AI (như 'Dưới đây là lời chúc...', 'Chào bạn...') hay bất kỳ lời giải thích nào.");
        } else {
            sb.append("\nStrict rules for generating the wish:\n");
            sb.append("1. LANGUAGE: Write 100% in fluent, natural English. Do not mix other languages.\n");
            sb.append("2. COMPLETENESS & LENGTH: Write a COMPLETE message with an opening, milestone reflections, and a warm closing (approx. 150-250 words, 2-3 short paragraphs). Never cut off mid-sentence.\n");
            sb.append("3. MILESTONE INTEGRATION: Seamlessly incorporate ALL provided milestones. Do not list them like a résumé or repeat identical praising phrases.\n");
            sb.append("4. AUTHENTIC TONE: Sincere, down-to-earth, and personal. Avoid corporate clichés or overly melodramatic phrasing.\n");
            sb.append("5. ADDRESSING: Respect sender and recipient pronouns/names exactly as specified.\n");
            sb.append("6. OUTPUT: Return ONLY the finished wish text itself, without any introductory meta-commentary or concluding remarks.");
        }

        return sb.toString();
    }
}
