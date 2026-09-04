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
import java.util.Collections;
import java.util.List;

@Component("groqClient")
@Slf4j
public class GroqClientImpl implements AiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public GroqClientImpl(
            RestClient restClient,
            ObjectMapper objectMapper,
            @Value("${app.ai.groq.api-key:mock-groq-key}") String apiKey,
            @Value("${app.ai.groq.model:openai/gpt-oss-120b}") String model,
            @Value("${app.ai.groq.base-url:https://api.groq.com}") String baseUrl
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
        for (String m : List.of("openai/gpt-oss-120b", "openai/gpt-oss-20b", "llama-3.1-8b-instant")) {
            if (!candidateModels.contains(m)) {
                candidateModels.add(m);
            }
        }

        Exception lastException = null;
        for (String targetModel : candidateModels) {
            try {
                log.info("Attempting wish generation via Groq model: {}", targetModel);
                return executeGroqCall(targetModel, fullPrompt, language);
            } catch (BusinessException be) {
                if (be.getStatus() == HttpStatus.NOT_FOUND) {
                    log.warn("Groq model '{}' not found (404). Trying next candidate...", targetModel);
                    lastException = be;
                    continue;
                }
                throw be;
            } catch (RuntimeException ex) {
                String msg = ex.getMessage() != null ? ex.getMessage() : "";
                if (msg.contains("404") || msg.contains("model_decommissioned") || msg.contains("does not exist") || msg.contains("no longer supported")) {
                    log.warn("Groq model '{}' unavailable: {}. Trying next candidate...", targetModel, msg);
                    lastException = ex;
                    continue;
                }
                throw ex;
            }
        }

        // If hardcoded candidates fail, dynamically discover currently active models from Groq API
        log.info("Querying Groq models catalog to dynamically discover active chat models...");
        List<String> dynamicModels = fetchAvailableGroqModels();
        for (String activeModel : dynamicModels) {
            if (candidateModels.contains(activeModel)) continue;
            try {
                log.info("Attempting wish generation via discovered Groq model: {}", activeModel);
                return executeGroqCall(activeModel, fullPrompt, language);
            } catch (Exception ex) {
                log.warn("Discovered Groq model '{}' failed: {}", activeModel, ex.getMessage());
                lastException = ex;
            }
        }

        throw new BusinessException("Groq failed for all candidate models: " + (lastException != null ? lastException.getMessage() : "Unknown"), HttpStatus.BAD_GATEWAY);
    }

    private String executeGroqCall(String targetModel, String fullPrompt, WishLanguage language) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", targetModel);
        payload.put("temperature", 0.7);
        payload.put("max_tokens", 2048);

        ArrayNode messages = payload.putArray("messages");

        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", language == WishLanguage.VI
                ? "Bạn là người viết lời chúc cá nhân hóa chân thành, gần gũi và sâu sắc. Luôn viết hoàn chỉnh bằng 100% tiếng Việt, độ dài vừa vặn khoảng 150-250 từ (2-3 đoạn ngắn), giản dị, không sáo rỗng hoa mỹ, tuyệt đối không cắt cụt câu."
                : "You are a thoughtful personal wish writer who crafts complete, concise, and heartfelt wishes (150-250 words, 2-3 short paragraphs) with genuine warmth, avoiding clichéd fluff.");

        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", fullPrompt);

        String url = String.format("%s/openai/v1/chat/completions", baseUrl);

        byte[] bytes = restClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.ALL)
                .body(payload)
                .exchange((request, response) -> {
                    byte[] bodyBytes = response.getBody().readAllBytes();
                    if (response.getStatusCode().isError()) {
                        String errorText = new String(bodyBytes, StandardCharsets.UTF_8);
                        log.warn("Groq model '{}' returned HTTP {}: {}", targetModel, response.getStatusCode(), errorText);
                        if (response.getStatusCode().value() == 404 || errorText.contains("404") || errorText.contains("does not exist") || errorText.contains("model_decommissioned")) {
                            throw new BusinessException("Groq model unavailable: " + errorText, HttpStatus.NOT_FOUND);
                        }
                        throw new BusinessException("Groq HTTP " + response.getStatusCode() + ": " + errorText, HttpStatus.BAD_GATEWAY);
                    }
                    return bodyBytes;
                });

        if (bytes == null || bytes.length == 0) {
            throw new BusinessException("Groq returned empty response", HttpStatus.BAD_GATEWAY);
        }

        String responseBody = new String(bytes, StandardCharsets.UTF_8);

        JsonNode responseNode;
        try {
            responseNode = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            throw new BusinessException("Failed to parse Groq response: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }

        JsonNode choice = responseNode.path("choices").get(0);
        if (choice != null && choice.has("message")) {
            String finishReason = choice.path("finish_reason").asText("");
            if ("length".equalsIgnoreCase(finishReason)) {
                log.warn("Groq model '{}' hit length limit!", targetModel);
            }
            String wishText = choice.path("message").path("content").asText().trim();
            if (!wishText.isBlank()) {
                log.info("Wish successfully generated via Groq model {} (length: {}, finishReason: {})",
                        targetModel, wishText.length(), finishReason);
                return wishText;
            }
        }

        throw new BusinessException("Groq returned an empty response", HttpStatus.BAD_GATEWAY);
    }

    private List<String> fetchAvailableGroqModels() {
        try {
            String url = String.format("%s/openai/v1/models", baseUrl);
            byte[] bytes = restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .accept(MediaType.APPLICATION_JSON, MediaType.ALL)
                    .exchange((request, response) -> response.getBody().readAllBytes());

            if (bytes != null && bytes.length > 0) {
                String resp = new String(bytes, StandardCharsets.UTF_8);
                JsonNode root = objectMapper.readTree(resp);
                JsonNode data = root.path("data");
                if (data.isArray()) {
                    List<String> list = new ArrayList<>();
                    for (JsonNode item : data) {
                        String id = item.path("id").asText();
                        if (!id.isBlank() && !id.contains("whisper") && !id.contains("tts") && !id.contains("guard") && !id.contains("embed")) {
                            list.add(id);
                        }
                    }
                    if (!list.isEmpty()) {
                        log.info("Found {} active Groq text models: {}", list.size(), list);
                        return list;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch dynamic Groq models list: {}", e.getMessage());
        }
        return Collections.emptyList();
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
