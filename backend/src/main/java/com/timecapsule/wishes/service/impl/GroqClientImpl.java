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
        payload.put("max_tokens", 450);

        ArrayNode messages = payload.putArray("messages");

        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", language == WishLanguage.VI
                ? "Bạn là người viết lời chúc chân thành, ngắn gọn và gần gũi. Tuyệt đối không dùng văn mẫu hoa mỹ, sáo rỗng hay viết dài dòng."
                : "You are a thoughtful personal wish writer who writes concise, genuine, and grounded wishes without clichéd fluff.");

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
            String wishText = choice.path("message").path("content").asText().trim();
            if (!wishText.isBlank()) {
                log.info("Wish successfully generated via Groq model {} (length: {})", targetModel, wishText.length());
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
