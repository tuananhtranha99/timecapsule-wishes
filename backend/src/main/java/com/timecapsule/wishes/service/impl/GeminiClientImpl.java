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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            } catch (Exception ex) {
                log.warn("Gemini model '{}' failed: {}. Trying next candidate...", targetModel, ex.getMessage());
                lastException = ex;
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
                        continue;
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

    private String extractField(String text, String regex, String defaultValue) {
        if (text == null) return defaultValue;
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return defaultValue;
    }

    private String buildPrompt(String prompt, List<String> milestones, WishLanguage language) {
        String recipientName = extractField(prompt, "Người nhận:\\s*([^.(,\n]+)", "bạn");
        String pronounSelf = extractField(prompt, "(?:Xưng|Xưng hô):\\s*([^.(,\n]+)", "");
        String pronounRecipient = extractField(prompt, "(?:Hô):\\s*([^.(,\n]+)", "");
        String occasion = extractField(prompt, "Dịp:\\s*([^.,\n]+)", language == WishLanguage.EN ? "Birthday" : "Sinh nhật");
        String tone = extractField(prompt, "Giọng điệu:\\s*([^.,\n]+)", language == WishLanguage.EN ? "Warm & Heartfelt" : "Ấm áp, chân thành");
        String notes = extractField(prompt, "Ghi chú cá nhân:\\s*([^.\n]+)", "");
        String customReq = extractField(prompt, "Yêu cầu bổ sung:\\s*([^\n]+)", "");

        StringBuilder sb = new StringBuilder();

        if (language == WishLanguage.VI) {
            sb.append("Bạn là một chuyên gia viết lời chúc cá nhân hóa hàng đầu, sâu sắc, tinh tế và dạt dào tình cảm chân thành.\n");
            sb.append("Nhiệm vụ: Hãy tạo một bức thông điệp lời chúc HOÀN CHỈNH, ĐẸP ĐẼ và ẤM ÁP nhân dịp ").append(occasion).append(".\n\n");

            sb.append("=== 1. THIẾT LẬP VAI TRÒ & QUY TẮC XƯNG HÔ (CỰC KỲ QUAN TRỌNG - TUÂN THỦ TUYỆT ĐỐI) ===\n");
            sb.append("- NGƯỜI NÓI / NGƯỜI GỬI LỜI CHÚC (Ngôi thứ nhất - Chủ thể phát ngôn):\n");
            if (!pronounSelf.isBlank()) {
                sb.append("  + Tự xưng là: \"").append(pronounSelf).append("\"\n");
                sb.append("  + Đây là người đang viết bức thông điệp này gửi tới đối phương. Ví dụ người viết sẽ xưng: \"")
                  .append(pronounSelf).append(" chúc...\", \"").append(pronounSelf)
                  .append(" rất tự hào về...\", \"Với ").append(pronounSelf).append(", em luôn là...\".\n");
            } else {
                sb.append("  + Tự xưng một cách tự nhiên theo ngữ cảnh (ví dụ: mình, tôi, anh, chị).\n");
            }

            sb.append("- NGƯỜI NHẬN LỜI CHÚC (Ngôi thứ hai - Người được chúc / Nhân vật chính của ngày hôm nay):\n");
            sb.append("  + Tên: \"").append(recipientName).append("\"\n");
            if (!pronounRecipient.isBlank()) {
                sb.append("  + Được người gửi gọi thân mật là: \"").append(pronounRecipient).append("\"\n");
                sb.append("  + Đây là người nhận được lời chúc hôm nay! Ví dụ người gửi sẽ gọi: \"Chúc mừng ")
                  .append(occasion).append(" ").append(pronounRecipient).append("!\", \"Chúc ")
                  .append(pronounRecipient).append(" tuổi mới...\", \"").append(pronounRecipient)
                  .append(" đã làm rất tốt...\".\n");
            } else {
                sb.append("  + Được gọi là \"").append(recipientName).append("\" hoặc xưng hô phù hợp.\n");
            }

            sb.append("- CHỦ NHÂN CỦA CÁC CỘT MỐC / THÀNH TỰU:\n");
            sb.append("  + MỌI CỘT MỐC ĐƯỢC LIỆT KÊ DƯỚI ĐÂY LÀ DO NGƯỜI NHẬN (")
              .append(!pronounRecipient.isBlank() ? pronounRecipient : recipientName)
              .append(") NỖ LỰC ĐẠT ĐƯỢC trong năm qua!\n");
            sb.append("  + Người gửi (").append(!pronounSelf.isBlank() ? pronounSelf : "người viết")
              .append(") KHÔNG PHẢI là người đạt được những điều đó, mà là người đồng hành, chứng kiến và tự hào về người nhận!\n");

            sb.append("- CẢNH BÁO CHỐNG LỖI ĐẢO NGƯỢC XƯNG HÔ (NGHIÊM CẤM):\n");
            if (!pronounSelf.isBlank() && !pronounRecipient.isBlank()) {
                sb.append("  + TUYỆT ĐỐI CẤM chúc người nhận bằng đại từ của người gửi (CẤM chúc: \"Chúc mừng sinh nhật, ")
                  .append(pronounSelf).append("\" hay \"Chúc ").append(pronounSelf).append(" luôn vui vẻ\").\n");
                sb.append("  + Người gửi (\"").append(pronounSelf).append("\") đang chúc người nhận (\"")
                  .append(pronounRecipient).append("\"). Bắt buộc người gửi xưng \"").append(pronounSelf)
                  .append("\" và gọi người nhận là \"").append(pronounRecipient).append("\"!\n");
            }

            sb.append("\n=== 2. THÔNG TIN BỐI CẢNH ===\n");
            sb.append("- Dịp: ").append(occasion).append("\n");
            sb.append("- Giọng điệu mong muốn: ").append(tone).append("\n");
            if (!notes.isBlank()) {
                sb.append("- Ghi chú thêm về người nhận: ").append(notes).append("\n");
            }
            if (!customReq.isBlank()) {
                sb.append("- Mong muốn riêng từ người gửi: ").append(customReq).append("\n");
            }

            sb.append("\n=== 3. CÁC CỘT MỐC ĐÃ ĐẠT ĐƯỢC (BẮT BUỘC ĐƯA VÀO LỜI CHÚC) ===\n");
            if (milestones != null && !milestones.isEmpty()) {
                sb.append("Người nhận đã đạt được / trải qua các cột mốc ý nghĩa sau trong năm qua:\n");
                for (String m : milestones) {
                    sb.append("- ").append(m).append("\n");
                }
            } else {
                sb.append("(Không có cột mốc cụ thể nào được chọn. Hãy viết lời chúc chân thành, ấm áp chung).\n");
            }

            sb.append("\n=== 4. QUY TẮC VIẾT LỜI CHÚC BẮT BUỘC ===\n");
            sb.append("1. NGÔN NGỮ: Viết 100% bằng TIẾNG VIỆT tự nhiên, tình cảm. Tuyệt đối KHÔNG chêm tiếng Anh (ví dụ: ngày sinh nhật thì dùng 'Chúc mừng sinh nhật...', KHÔNG dùng 'Happy birthday').\n");
            sb.append("2. TUYỆT ĐỐI KHÔNG ĐỌC NGÀY THÁNG MÁY MÓC: Tuyệt đối không chép ngày tháng kiểu robot (như 'vào ngày 4-9-2026...', 'ngày 3-3-2026...'). Hãy điểm lại các cột mốc một cách mượt mà theo dòng chảy thời gian của năm qua (ví dụ: 'từ lúc tốt nghiệp đại học, rồi giành được suất học bổng, cho đến khi tự sắm được chiếc xe máy mới...').\n");
            sb.append("3. LỐI VIẾT CHÂN THÀNH, ĐỜI THƯỜNG: Giản dị, ấm áp như lời nhắn gửi giữa những người thân thiết ngoài đời thật. KHÔNG dùng văn mẫu sáo rỗng, KHÔNG đao to búa lớn, KHÔNG dùng từ hoa mỹ cường điệu (như 'minh chứng cho tài năng không ngừng', 'dấu hiệu của sự quyết tâm tiến bước').\n");
            sb.append("4. TÍNH TRỌN VẸN & ĐỘ DÀI: Viết một bức thông điệp HOÀN CHỈNH gồm: (1) Lời mở đầu ấm áp chào đón dịp đặc biệt, (2) Đoạn chia sẻ, điểm lại các cột mốc với sự tự hào/yêu thương, (3) Lời chúc kết thúc đong đầy hy vọng cho tuổi mới/chặng đường mới. Độ dài khoảng 150 đến 250 từ (2 đến 3 đoạn văn ngắn). Tuyệt đối không ngắt cụt lửng.\n");
            sb.append("5. ĐẦU RA DUY NHẤT: CHỈ trả về đúng nội dung lời chúc hoàn chỉnh. KHÔNG thêm bất kỳ lời chào mở đầu hay giải thích nào của AI.");
        } else {
            sb.append("You are an expert personal wish writer who crafts warm, heartfelt, and memorable messages.\n");
            sb.append("Task: Write a COMPLETE, BEAUTIFUL, and HEARTFELT message for ").append(occasion).append(".\n\n");

            sb.append("=== 1. ROLES & PRONOUN RULES (STRICT COMPLIANCE REQUIRED) ===\n");
            sb.append("- SENDER (First person - Speaker):\n");
            if (!pronounSelf.isBlank()) {
                sb.append("  + Refers to self as: \"").append(pronounSelf).append("\"\n");
            } else {
                sb.append("  + Refers to self naturally (e.g. \"I\").\n");
            }

            sb.append("- RECIPIENT (Second person - Honoree/Celebrant):\n");
            sb.append("  + Name: \"").append(recipientName).append("\"\n");
            if (!pronounRecipient.isBlank()) {
                sb.append("  + Addressed as: \"").append(pronounRecipient).append("\"\n");
            } else {
                sb.append("  + Addressed as \"").append(recipientName).append("\" or natural second person.\n");
            }

            sb.append("- OWNER OF MILESTONES: ALL milestones listed below were achieved by the RECIPIENT (")
              .append(!pronounRecipient.isBlank() ? pronounRecipient : recipientName).append(")!\n");
            sb.append("  The sender is expressing proud support and affection for the recipient.\n");

            sb.append("- CRITICAL: NEVER reverse the pronouns (do not wish the sender a happy birthday!).\n");

            sb.append("\n=== 2. CONTEXT ===\n");
            sb.append("- Occasion: ").append(occasion).append("\n");
            sb.append("- Tone: ").append(tone).append("\n");
            if (!notes.isBlank()) {
                sb.append("- Personal notes: ").append(notes).append("\n");
            }
            if (!customReq.isBlank()) {
                sb.append("- Custom request: ").append(customReq).append("\n");
            }

            sb.append("\n=== 3. MILESTONES (ALL MUST BE INTEGRATED) ===\n");
            if (milestones != null && !milestones.isEmpty()) {
                for (String m : milestones) {
                    sb.append("- ").append(m).append("\n");
                }
            } else {
                sb.append("(No specific milestones. Write a warm general message).\n");
            }

            sb.append("\n=== 4. WRITING RULES ===\n");
            sb.append("1. LANGUAGE: 100% natural, fluent English.\n");
            sb.append("2. DO NOT RECITE DATES ROBOTICALLY: Weave events smoothly without mechanical timestamp phrasing.\n");
            sb.append("3. TONE: Sincere, down-to-earth, and personal. Avoid stiff corporate jargon or exaggerated clichés.\n");
            sb.append("4. COMPLETENESS & LENGTH: A complete 3-part wish (warm greeting, milestone reflections, uplifting closing), approx. 150-250 words (2-3 short paragraphs). Never truncate.\n");
            sb.append("5. OUTPUT: Output ONLY the final wish text.");
        }

        return sb.toString();
    }
}
