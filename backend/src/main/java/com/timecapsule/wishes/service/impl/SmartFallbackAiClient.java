package com.timecapsule.wishes.service.impl;

import com.timecapsule.wishes.enums.ToneStyle;
import com.timecapsule.wishes.enums.WishLanguage;
import com.timecapsule.wishes.service.AiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

        String recipientName = extractField(prompt, "Người nhận:\\s*([^.(,\n]+)", "bạn");
        String pronounSelf = extractField(prompt, "(?:Xưng|Xưng hô):\\s*([^.(,\n]+)", "");
        String pronounRecipient = extractField(prompt, "(?:Hô):\\s*([^.(,\n]+)", "");
        String toneStr = extractField(prompt, "Giọng điệu:\\s*([A-Za-z_]+)", "WARM");
        String occasion = extractField(prompt, "Dịp:\\s*([^.,\n]+)", "Sinh nhật");

        ToneStyle tone = parseTone(toneStr);
        List<String> cleanedMilestones = cleanMilestones(milestones);

        if (language == WishLanguage.EN) {
            return generateEnglishWish(recipientName, pronounSelf, pronounRecipient, tone, occasion, cleanedMilestones);
        } else {
            return generateVietnameseWish(recipientName, pronounSelf, pronounRecipient, tone, occasion, cleanedMilestones);
        }
    }

    private String generateVietnameseWish(
            String name,
            String pronounSelfRaw,
            String pronounRecipientRaw,
            ToneStyle tone,
            String occasion,
            List<String> milestones
    ) {
        String self = (pronounSelfRaw != null && !pronounSelfRaw.isBlank()) ? pronounSelfRaw.trim() : "mình";
        String rec = (pronounRecipientRaw != null && !pronounRecipientRaw.isBlank()) ? pronounRecipientRaw.trim() : "bạn";

        StringBuilder sb = new StringBuilder();

        // 1. Salutation
        sb.append(buildVietnameseSalutation(name, rec, tone)).append("\n\n");

        // 2. Occasion Intro
        sb.append(buildVietnameseOccasionIntro(name, self, rec, occasion, tone)).append("\n\n");

        // 3. Milestones synthesis
        sb.append(buildVietnameseMilestones(self, rec, milestones, tone)).append("\n\n");

        // 4. Closing wishes
        sb.append(buildVietnameseClosing(self, rec, tone));

        return sb.toString().trim();
    }

    private String buildVietnameseSalutation(String name, String rec, ToneStyle tone) {
        return switch (tone) {
            case SWEET -> pickOne(List.of(
                    name + " yêu dấu ơi,",
                    "Gửi " + rec + " yêu thương của " + name + ",",
                    rec + " " + name + " của lòng mình ơi,"
            ));
            case PLAYFUL -> pickOne(List.of(
                    "Alo " + name + "!",
                    "Gửi đồng chí " + name + "!",
                    "Chào " + rec + " " + name + " iu quý,"
            ));
            case RESPECTFUL -> pickOne(List.of(
                    "Kính gửi " + name + ",",
                    "Kính chúc " + name + ",",
                    "Thân gửi " + name + ","
            ));
            case CASUAL -> pickOne(List.of(
                    "Chào " + name + " nhé,",
                    "Hey " + name + "!",
                    name + " ơi,"
            ));
            case WARM -> pickOne(List.of(
                    "Thân gửi " + name + ",",
                    "Gửi " + name + " thân mến,",
                    name + " ơi,"
            ));
        };
    }

    private String buildVietnameseOccasionIntro(String name, String self, String rec, String occasion, ToneStyle tone) {
        String lowerOccasion = occasion.toLowerCase();
        boolean isBirthday = lowerOccasion.contains("sinh nhật") || lowerOccasion.contains("birthday");
        boolean isTet = lowerOccasion.contains("tết") || lowerOccasion.contains("năm mới") || lowerOccasion.contains("new year");
        boolean isAnniversary = lowerOccasion.contains("kỷ niệm") || lowerOccasion.contains("anniversary");

        if (isBirthday) {
            return switch (tone) {
                case SWEET -> "Hôm nay là sinh nhật của " + rec + " rồi! " + capitalize(self) + " chúc " + rec + " bước sang tuổi mới luôn ngập tràn niềm vui, nụ cười rạng rỡ và luôn cảm nhận được tình yêu thương đong đầy nhất.";
                case PLAYFUL -> "Lại thêm một tuổi mới nữa rồi nè! Chúc " + rec + " tuổi mới nhan sắc thăng hạng, tiền tài phơi phới, bớt lo nghĩ và lúc nào cũng vui hết nấc nhé!";
                case RESPECTFUL -> "Nhân ngày sinh nhật của " + rec + ", " + self + " xin kính chúc " + rec + " thêm một tuổi mới thật nhiều sức khỏe, bình an, vạn sự hanh thông và tràn ngập niềm vui.";
                case CASUAL -> "Chúc mừng sinh nhật " + rec + " nha! Chúc " + rec + " tuổi mới thật nhiều may mắn, làm gì cũng thuận lợi và luôn giữ được tinh thần lạc quan.";
                case WARM -> "Chúc mừng sinh nhật " + name + "! " + capitalize(self) + " chúc " + rec + " một tuổi mới thật an yên, hạnh phúc và gặt hái được mọi điều mà " + rec + " hằng ấp ủ.";
            };
        } else if (isTet) {
            return switch (tone) {
                case SWEET -> "Năm mới Tết đến, " + self + " chúc " + rec + " luôn rạng ngời, bình an và một năm trọn vẹn yêu thương bên " + self + " cùng gia đình.";
                case PLAYFUL -> "Tết đến xuân sang, chúc " + rec + " ví dày cộm, việc nhàn lương cao, ăn mãi không béo và luôn tươi như hoa!";
                case RESPECTFUL -> "Nhân dịp đầu xuân năm mới, " + self + " kính chúc " + rec + " và gia đình an khang thịnh vượng, dồi dào sức khỏe, vạn sự cát tường.";
                case CASUAL -> "Tết đến rồi, chúc " + rec + " một năm mới tràn đầy năng lượng, nhiều may mắn và thành công rực rỡ!";
                case WARM -> "Nhân dịp năm mới Tết sang, chúc " + rec + " và gia đình một năm an vui, đầm ấm và thật nhiều khởi sắc.";
            };
        } else if (isAnniversary) {
            return switch (tone) {
                case SWEET -> "Chúc mừng ngày kỷ niệm thật đặc biệt của chúng mình! Cảm ơn " + rec + " vì đã luôn ở bên, lắng nghe và chia sẻ cùng " + self + " qua từng khoảnh khắc.";
                case PLAYFUL -> "Chúc mừng ngày kỷ niệm! Thật may vì chúng ta vẫn chịu đựng được nhau suốt thời gian qua mà vẫn thấy vui, cùng nhau quậy tiếp nhé!";
                case RESPECTFUL -> "Nhân ngày kỷ niệm ý nghĩa này, " + self + " xin gửi tới " + rec + " lời chúc mừng trân trọng nhất cùng lời tri ân sâu sắc.";
                case CASUAL -> "Chúc mừng ngày kỷ niệm nhé! Nhìn lại thời gian qua thật đáng nhớ, chúc chúng ta ngày càng gắn kết và nhiều kỷ niệm đẹp hơn nữa.";
                case WARM -> "Chúc mừng ngày kỷ niệm thật ý nghĩa! Trân trọng từng bước đường và những kỷ niệm quý giá mà chúng ta đã cùng nhau trải qua.";
            };
        } else {
            return "Nhân dịp " + occasion + " đặc biệt này, " + self + " muốn gửi tới " + rec + " những lời chúc tốt đẹp, chân thành và trọn vẹn nhất.";
        }
    }

    private String buildVietnameseMilestones(String self, String rec, List<String> milestones, ToneStyle tone) {
        if (milestones == null || milestones.isEmpty()) {
            return switch (tone) {
                case SWEET -> "Dù một năm qua có những ngày bận rộn hay êm đềm, " + self + " luôn thấy ấm lòng khi được dõi theo từng ngày của " + rec + ". " + capitalize(rec) + " hãy luôn tự tin và tỏa sáng theo cách của riêng mình nhé.";
                case PLAYFUL -> "Năm vừa rồi cày cuốc cũng chăm chỉ lắm rồi, giờ là lúc tận hưởng ngày vui của mình thôi!";
                case RESPECTFUL -> "Nhìn lại chặng đường một năm với biết bao nỗ lực và sự kiên trì, " + self + " luôn dành sự trân trọng sâu sắc cho " + rec + ".";
                case CASUAL -> "Năm vừa rồi chắc hẳn cũng có nhiều kỷ niệm đáng nhớ, chúc " + rec + " bước tiếp chặng đường tới thật hứng khởi!";
                case WARM -> "Nhìn lại một năm qua, " + self + " rất trân quý những nỗ lực thầm lặng và nguồn năng lượng tích cực mà " + rec + " luôn giữ trong mình.";
            };
        }

        StringBuilder sb = new StringBuilder();
        if (milestones.size() == 1) {
            String m = milestones.get(0);
            switch (tone) {
                case SWEET -> sb.append("Nhìn lại năm qua, ").append(self).append(" thực sự tự hào khi nhớ về khoảnh khắc ")
                        .append(rec).append(" đã ").append(m).append(". Đó là dấu mốc minh chứng cho bản lĩnh và sự nỗ lực tuyệt vời của ").append(rec).append(".");
                case PLAYFUL -> sb.append("Năm qua nể nhất là quả thành tích ").append(rec).append(" đã ").append(m)
                        .append(" — đỉnh thực sự, không đùa được đâu!");
                case RESPECTFUL -> sb.append("Dấu mốc đáng ghi nhận khi ").append(rec).append(" đã ").append(m)
                        .append(" trong năm qua thực sự là một thành quả đáng tự hào cho sự kiên định bền bỉ.");
                case CASUAL -> sb.append("Năm vừa rồi quả thật có điểm nhấn khó quên khi ").append(rec).append(" đã ")
                        .append(m).append(" — bước tiến rất chất lượng!");
                case WARM -> sb.append("Một năm đáng nhớ với cột mốc ").append(rec).append(" đã ").append(m)
                        .append(", minh chứng rõ nét cho sự cố gắng không ngừng nghỉ của ").append(rec).append(".");
            }
        } else {
            String first = milestones.get(0);
            String last = milestones.get(milestones.size() - 1);
            switch (tone) {
                case SWEET -> {
                    sb.append("Dõi theo chặng đường của ").append(rec).append(" suốt năm qua, từ lúc ").append(first);
                    if (milestones.size() > 2) {
                        for (int i = 1; i < milestones.size() - 1; i++) {
                            sb.append(", rồi đến khi ").append(milestones.get(i));
                        }
                    }
                    sb.append(", cho đến khi ").append(last).append(" — ").append(self).append(" càng thêm yêu thương và khâm phục sự kiên cường của ").append(rec).append(".");
                }
                case PLAYFUL -> {
                    sb.append("Năm qua nhìn lại bảng thành tích cũng ra gì phết: từ việc ").append(first);
                    if (milestones.size() > 2) {
                        for (int i = 1; i < milestones.size() - 1; i++) {
                            sb.append(", rồi ").append(milestones.get(i));
                        }
                    }
                    sb.append(", tới cú chốt ").append(last).append(". Quá đỉnh luôn!");
                }
                case RESPECTFUL -> {
                    sb.append("Những nỗ lực đáng trân trọng trong năm qua, từ việc ").append(first);
                    if (milestones.size() > 2) {
                        for (int i = 1; i < milestones.size() - 1; i++) {
                            sb.append(", cùng với ").append(milestones.get(i));
                        }
                    }
                    sb.append(", đến dấu ấn ").append(last).append(" — đều thể hiện rõ tinh thần tận tụy và trách nhiệm của ").append(rec).append(".");
                }
                case CASUAL -> {
                    sb.append("Một năm đầy ắp trải nghiệm, từ lúc ").append(first);
                    if (milestones.size() > 2) {
                        for (int i = 1; i < milestones.size() - 1; i++) {
                            sb.append(", rồi ").append(milestones.get(i));
                        }
                    }
                    sb.append(", cho đến lúc ").append(last).append(". Cứ thế phát huy nhé!");
                }
                case WARM -> {
                    sb.append("Nhìn lại những cột mốc ý nghĩa một năm qua, từ khi ").append(rec).append(" ").append(first);
                    if (milestones.size() > 2) {
                        for (int i = 1; i < milestones.size() - 1; i++) {
                            sb.append(", tiếp nối là ").append(milestones.get(i));
                        }
                    }
                    sb.append(", cho đến dấu ấn ").append(last).append(" — mỗi bước đi đều mang đậm dấu ấn nỗ lực và đam mê của ").append(rec).append(".");
                }
            }
        }
        return sb.toString();
    }

    private String buildVietnameseClosing(String self, String rec, ToneStyle tone) {
        return switch (tone) {
            case SWEET -> pickOne(List.of(
                    "Chúc cho mọi dự định sắp tới của " + rec + " đều đơm hoa kết trái. " + capitalize(self) + " sẽ luôn ở đây ủng hộ và yêu thương " + rec + "! ❤️",
                    "Thương chúc " + rec + " luôn rạng rỡ, bình an và hạnh phúc trong từng ngày. Yêu " + rec + " rất nhiều! ✨",
                    "Mong rằng tuổi mới sẽ mang đến cho " + rec + " thật nhiều nụ cười và những điều dịu dàng nhất. Luôn bên " + rec + "! 💕"
            ));
            case PLAYFUL -> pickOne(List.of(
                    "Tuổi mới chúc " + rec + " ví dày, việc nhẹ, mau giàu để bao " + self + " đi ăn nhé! 🍻🎉",
                    "Cứ mãi tươi trẻ, lầy lội và rực rỡ như thế nhé. Mãi đỉnh nha! 🚀😎",
                    "Chúc " + rec + " vạn sự như ý, tỷ sự như mơ, không âu lo mà chỉ toàn niềm vui thôi! 🥳"
            ));
            case RESPECTFUL -> pickOne(List.of(
                    "Kính chúc " + rec + " luôn dồi dào sức khỏe, gia đạo bình an và đạt thêm nhiều thành tựu rực rỡ hơn nữa.",
                    "Kính mong " + rec + " luôn an vui, vạn sự hanh thông và gặp nhiều may mắn trên mọi bước đường.",
                    "Xin gửi tới " + rec + " lời chúc sức khỏe, an lạc và hạnh phúc viên mãn."
            ));
            case CASUAL -> pickOne(List.of(
                    "Chúc " + rec + " năm mới vạn sự thuận lợi, việc gì cũng hanh thông và luôn giữ lửa nhiệt huyết nhé!",
                    "Cứ vui tươi và tận hưởng chặng đường tiếp theo thật trọn vẹn nhé " + rec + "!",
                    "Chúc mọi điều may mắn và tốt lành nhất sẽ đến với " + rec + " trong thời gian tới!"
            ));
            case WARM -> pickOne(List.of(
                    "Chúc cho chặng đường phía trước của " + rec + " luôn ngập tràn ánh sáng, may mắn và bình yên. Cảm ơn vì những năng lượng tuyệt vời mà " + rec + " luôn mang lại! ✨",
                    "Bước sang trang mới, chúc " + rec + " luôn dồi dào sức khỏe, tâm an yên và chạm tới mọi ước mơ của mình nhé.",
                    "Thương chúc " + rec + " tất cả những điều tốt đẹp và dịu dàng nhất của cuộc sống. Luôn mỉm cười thật tươi nhé!"
            ));
        };
    }

    private String generateEnglishWish(
            String name,
            String pronounSelfRaw,
            String pronounRecipientRaw,
            ToneStyle tone,
            String occasion,
            List<String> milestones
    ) {
        String self = (pronounSelfRaw != null && !pronounSelfRaw.isBlank()) ? pronounSelfRaw.trim() : "I";
        String rec = (pronounRecipientRaw != null && !pronounRecipientRaw.isBlank()) ? pronounRecipientRaw.trim() : "you";

        StringBuilder sb = new StringBuilder();

        // 1. Salutation
        sb.append(buildEnglishSalutation(name, tone)).append("\n\n");

        // 2. Intro
        sb.append(buildEnglishOccasionIntro(name, self, rec, occasion, tone)).append("\n\n");

        // 3. Milestones
        sb.append(buildEnglishMilestones(self, rec, milestones, tone)).append("\n\n");

        // 4. Closing
        sb.append(buildEnglishClosing(self, rec, tone));

        return sb.toString().trim();
    }

    private String buildEnglishSalutation(String name, ToneStyle tone) {
        return switch (tone) {
            case SWEET -> pickOne(List.of(
                    "My dearest " + name + ",",
                    "Dearest " + name + ",",
                    "To my beloved " + name + ","
            ));
            case PLAYFUL -> pickOne(List.of(
                    "Hey " + name + "!",
                    "Happy celebration to the one and only " + name + "!",
                    "What's up, " + name + "!"
            ));
            case RESPECTFUL -> pickOne(List.of(
                    "Dear " + name + ",",
                    "Warmest regards, " + name + ",",
                    "To " + name + ","
            ));
            case CASUAL -> pickOne(List.of(
                    "Hey " + name + "!",
                    "Hi " + name + ",",
                    "Dear " + name + ","
            ));
            case WARM -> pickOne(List.of(
                    "Dear " + name + ",",
                    "Warmest wishes to " + name + ",",
                    "To dear " + name + ","
            ));
        };
    }

    private String buildEnglishOccasionIntro(String name, String self, String rec, String occasion, ToneStyle tone) {
        return switch (tone) {
            case SWEET -> "On this special occasion of your " + occasion + ", my heart is full of gratitude for having " + rec + " in my life.";
            case PLAYFUL -> "Happy " + occasion + "! Another year bolder, wiser, and definitely more legendary!";
            case RESPECTFUL -> "On this distinguished occasion of your " + occasion + ", please accept my sincere wishes for health, prosperity, and peace.";
            case CASUAL -> "Wishing " + rec + " a very happy " + occasion + "! Hope it's filled with great vibes and good times.";
            case WARM -> "Sending " + rec + " the warmest wishes on this special " + occasion + "! May your day be filled with joy and love.";
        };
    }

    private String buildEnglishMilestones(String self, String rec, List<String> milestones, ToneStyle tone) {
        if (milestones == null || milestones.isEmpty()) {
            return switch (tone) {
                case SWEET -> "Every step of your journey inspires me, and I cherish watching your spirit shine so brightly.";
                case PLAYFUL -> "You've survived another crazy year with style — time to celebrate big!";
                case RESPECTFUL -> "Looking back upon the accomplishments of this past year, your dedication has been admirable.";
                case CASUAL -> "It's been quite a ride this past year, and you've navigated it with true resilience.";
                case WARM -> "Reflecting on the past year, your kindness, warmth, and resilience continue to inspire everyone around " + rec + ".";
            };
        }

        StringBuilder sb = new StringBuilder();
        if (milestones.size() == 1) {
            String m = milestones.get(0);
            sb.append("Looking back at this past year, ").append(self).append(" is so proud of when ")
                    .append(rec).append(" ").append(m).append(" — a true reflection of your perseverance and courage.");
        } else {
            String first = milestones.get(0);
            String last = milestones.get(milestones.size() - 1);
            sb.append("What an eventful year it has been: from ").append(first);
            if (milestones.size() > 2) {
                for (int i = 1; i < milestones.size() - 1; i++) {
                    sb.append(", to ").append(milestones.get(i));
                }
            }
            sb.append(", all the way to ").append(last).append("! Every achievement speaks volumes about your hard work.");
        }
        return sb.toString();
    }

    private String buildEnglishClosing(String self, String rec, ToneStyle tone) {
        return switch (tone) {
            case SWEET -> pickOne(List.of(
                    "With all my love and heart, always right by your side ❤️",
                    "Wishing you endless happiness and gentle moments. Love you always! ✨",
                    "May all your sweetest dreams come true. Forever cheering for you! 💕"
            ));
            case PLAYFUL -> pickOne(List.of(
                    "Keep being awesome (and don't forget to treat me to a drink)! 🍻🎉",
                    "Stay cool, stay legendary, and let's make this year unforgettable! 🚀",
                    "Cheers to more crazy adventures and fewer headaches ahead! 🥳"
            ));
            case RESPECTFUL -> pickOne(List.of(
                    "Wishing " + rec + " continued success, good health, and tranquility in all endeavors.",
                    "May this upcoming year bring peace, fulfillment, and prosperity to " + rec + " and your family.",
                    "With sincere respect and best wishes for the journey ahead."
            ));
            case CASUAL -> pickOne(List.of(
                    "Wishing " + rec + " all the best in the year ahead. Have a blast!",
                    "Hope this new chapter brings lots of fun, laughter, and great moments!",
                    "Cheers to what's next — keep up the fantastic work!"
            ));
            case WARM -> pickOne(List.of(
                    "May this next chapter bring " + rec + " boundless joy, good health, and wonderful memories! ✨",
                    "Wishing " + rec + " peace of mind and success in everything you set out to achieve. Warmest regards!",
                    "Here's to a bright and fulfilling road ahead. Keep shining bright! 🌟"
            ));
        };
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

    private ToneStyle parseTone(String toneStr) {
        if (toneStr == null || toneStr.isBlank()) {
            return ToneStyle.WARM;
        }
        try {
            return ToneStyle.valueOf(toneStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ToneStyle.WARM;
        }
    }

    private String pickOne(List<String> options) {
        return options.get(random.nextInt(options.size()));
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
