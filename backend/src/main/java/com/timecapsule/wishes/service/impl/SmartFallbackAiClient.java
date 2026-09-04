package com.timecapsule.wishes.service.impl;

import com.timecapsule.wishes.enums.WishLanguage;
import com.timecapsule.wishes.service.AiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("smartFallbackClient")
@Slf4j
public class SmartFallbackAiClient implements AiClient {

    private final Random random = new Random();

    @Override
    public String generateWish(String prompt, List<String> milestones, WishLanguage language) {
        log.info("Generating wish via Smart Heuristic AI Synthesizer (milestones count: {}, language: {})",
                milestones != null ? milestones.size() : 0, language);

        String recipientName = extractField(prompt, "Người nhận:\\s*([^(.,\n]+)", "bạn");
        String relationship = extractField(prompt, "Mối quan hệ:\\s*([^),\n]+)", "");
        String occasion = extractField(prompt, "Dịp:\\s*([^.,\n]+)", "Sinh nhật");
        String extraContext = extractField(prompt, "Yêu cầu bổ sung:\\s*([^\n]+)", "");

        List<String> cleanedMilestones = cleanMilestones(milestones);

        if (language == WishLanguage.EN) {
            return generateEnglishWish(recipientName, relationship, occasion, extraContext, cleanedMilestones);
        } else {
            return generateVietnameseWish(recipientName, relationship, occasion, extraContext, cleanedMilestones);
        }
    }

    private String generateVietnameseWish(
            String name,
            String relationship,
            String occasion,
            String extraContext,
            List<String> milestones
    ) {
        StringBuilder sb = new StringBuilder();

        // 1. Salutation & Opening
        String salutation = buildVietnameseSalutation(name, relationship);
        sb.append(salutation).append("\n\n");

        // 2. Occasion message
        sb.append(buildVietnameseOccasionIntro(name, occasion));
        sb.append("\n\n");

        // 3. Synthesizing Milestones
        if (milestones != null && !milestones.isEmpty()) {
            sb.append("Nhìn lại chặng đường một năm vừa qua, mình thực sự ấn tượng và tự hào về những cột mốc tuyệt vời mà bạn đã nỗ lực đạt được: ");
            if (milestones.size() == 1) {
                sb.append("đặc biệt là khoảnh khắc bạn đã ").append(milestones.get(0)).append(". ");
                sb.append("Đó không chỉ là một dấu ấn đáng nhớ, mà còn là minh chứng rõ nét cho sự cố gắng bền bỉ và bản lĩnh của bạn.");
            } else {
                sb.append("từ việc ").append(milestones.get(0));
                for (int i = 1; i < milestones.size(); i++) {
                    if (i == milestones.size() - 1) {
                        sb.append(", cho đến dấu ấn đáng tự hào khi ").append(milestones.get(i)).append(". ");
                    } else {
                        sb.append(", rồi đến khi ").append(milestones.get(i));
                    }
                }
                sb.append("Mỗi bước đi đều ghi dấu sự kiên trì, đam mê và nhiệt huyết không ngừng nghỉ.");
            }
            sb.append("\n\n");
        } else {
            sb.append("Dù một năm qua có những ngày bình yên hay bận rộn với bao bộn bề công việc, bạn vẫn luôn giữ được sự tích cực, ấm áp và nguồn năng lượng tuyệt vời lan tỏa đến mọi người xung quanh.\n\n");
        }

        // 4. Personal note / Extra context
        if (extraContext != null && !extraContext.isBlank()) {
            sb.append("Đặc biệt, ").append(extraContext.trim()).append(".\n\n");
        }

        // 5. Inspiring Closing Wishes
        sb.append(buildVietnameseClosingWishes(name, occasion));

        return sb.toString().trim();
    }

    private String generateEnglishWish(
            String name,
            String relationship,
            String occasion,
            String extraContext,
            List<String> milestones
    ) {
        StringBuilder sb = new StringBuilder();

        // 1. Salutation
        sb.append("Dear ").append(name).append(",\n\n");

        // 2. Occasion Intro
        sb.append("On this special occasion of your ").append(occasion).append(", I want to send you the warmest and most heartfelt wishes!\n\n");

        // 3. Milestones
        if (milestones != null && !milestones.isEmpty()) {
            sb.append("Looking back at the past year, I am so inspired and proud of the remarkable milestones you have accomplished: ");
            if (milestones.size() == 1) {
                sb.append("especially when you ").append(milestones.get(0)).append(". ");
                sb.append("It truly highlights your dedication, resilience, and passion.");
            } else {
                sb.append("from ").append(milestones.get(0));
                for (int i = 1; i < milestones.size(); i++) {
                    if (i == milestones.size() - 1) {
                        sb.append(", to celebrating ").append(milestones.get(i)).append(". ");
                    } else {
                        sb.append(", to ").append(milestones.get(i));
                    }
                }
                sb.append("Every single achievement is a testament to your hard work and beautiful spirit.");
            }
            sb.append("\n\n");
        } else {
            sb.append("Through every challenge and triumph this past year, your kindness, strength, and infectious positivity have continued to shine brightly.\n\n");
        }

        // 4. Extra Context
        if (extraContext != null && !extraContext.isBlank()) {
            sb.append("Also, ").append(extraContext.trim()).append(".\n\n");
        }

        // 5. Closing
        sb.append("May this new chapter bring you boundless happiness, great health, and even greater successes ahead. Keep shining and reaching for the stars!\n\n");
        sb.append("With all my love and best wishes,\nAlways by your side ❤️");

        return sb.toString().trim();
    }

    private String buildVietnameseSalutation(String name, String relationship) {
        String lowerRel = relationship.toLowerCase();
        if (lowerRel.contains("yêu") || lowerRel.contains("partner") || lowerRel.contains("vợ") || lowerRel.contains("chồng")) {
            return "Gửi " + name + " yêu thương,";
        } else if (lowerRel.contains("bạn") || lowerRel.contains("friend")) {
            return "Gửi người bạn tuyệt vời của mình, " + name + "!";
        } else if (lowerRel.contains("anh") || lowerRel.contains("chị") || lowerRel.contains("em")) {
            return "Thân gửi " + name + ",";
        }
        return "Thân gửi " + name + ",";
    }

    private String buildVietnameseOccasionIntro(String name, String occasion) {
        String lowerOccasion = occasion.toLowerCase();
        if (lowerOccasion.contains("sinh nhật") || lowerOccasion.contains("birthday")) {
            return "Nhân ngày sinh nhật đặc biệt của bạn, chúc bạn bước sang một tuổi mới thật rực rỡ, tràn ngập tiếng cười, niềm vui và luôn được yêu thương!";
        } else if (lowerOccasion.contains("tết") || lowerOccasion.contains("năm mới") || lowerOccasion.contains("new year")) {
            return "Nhân dịp năm mới Tết đến, chúc bạn và gia đình vạn sự hanh thông, an khang thịnh vượng, sức khỏe dồi dào và gặt hái thêm nhiều thành công mới!";
        } else if (lowerOccasion.contains("kỷ niệm") || lowerOccasion.contains("anniversary")) {
            return "Chúc mừng ngày kỷ niệm thật ý nghĩa! Cảm ơn vì tất cả những khoảnh khắc đẹp đẽ mà chúng ta đã cùng nhau trải qua.";
        }
        return "Nhân dịp " + occasion + " đặc biệt này, mình muốn gửi tới bạn những lời chúc chân thành, ấm áp và tốt đẹp nhất từ tận đáy lòng.";
    }

    private String buildVietnameseClosingWishes(String name, String occasion) {
        List<String> closings = List.of(
                "Chúc cho chặng đường phía trước của bạn luôn ngập tràn ánh nắng, may mắn song hành và mọi ước mơ đều sớm trở thành hiện thực. Hãy luôn giữ nụ cười tươi sáng và tinh thần lạc quan như bạn vẫn luôn thế nhé!\n\nThân ái & Yêu quý bạn rất nhiều! ✨",
                "Bước sang trang mới của cuộc đời, chúc bạn luôn dồi dào sức khỏe, tâm an yên, công việc hanh thông và đón nhận thêm nhiều cơ hội rực rỡ hơn nữa.\n\nLuôn ủng hộ và đồng hành cùng bạn! 🎉",
                "Cảm ơn vì đã luôn là một người tuyệt vời trong cuộc sống của mình. Chúc cho mọi dự định và khát vọng của bạn trong năm tới đều đơm hoa kết trái.\n\nThương chúc tất cả những điều tốt đẹp nhất! ❤️"
        );
        return closings.get(random.nextInt(closings.size()));
    }

    private List<String> cleanMilestones(List<String> rawMilestones) {
        if (rawMilestones == null || rawMilestones.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> cleaned = new ArrayList<>();
        Pattern pattern = Pattern.compile("^(.*?)(?:\\s*\\(ngày.*)?$");

        for (String m : rawMilestones) {
            if (m == null || m.isBlank()) continue;
            Matcher matcher = pattern.matcher(m.trim());
            if (matcher.find()) {
                String text = matcher.group(1).trim();
                // Lowercase the first char for smooth grammatical flow if needed
                if (!text.isEmpty()) {
                    cleaned.add(text);
                }
            } else {
                cleaned.add(m.trim());
            }
        }
        return cleaned;
    }

    private String extractField(String text, String regex, String defaultValue) {
        if (text == null || text.isBlank()) return defaultValue;
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String val = matcher.group(1).trim();
            return val.isEmpty() ? defaultValue : val;
        }
        return defaultValue;
    }
}
