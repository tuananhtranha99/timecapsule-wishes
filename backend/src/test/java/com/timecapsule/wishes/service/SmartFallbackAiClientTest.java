package com.timecapsule.wishes.service;

import com.timecapsule.wishes.enums.WishLanguage;
import com.timecapsule.wishes.service.impl.SmartFallbackAiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SmartFallbackAiClientTest {

    private final SmartFallbackAiClient client = new SmartFallbackAiClient();

    @Test
    @DisplayName("Should generate sweet Vietnamese wish with anh/em pronouns for partner")
    void testGenerateVietnameseWish_SweetTone_AnhEm() {
        String prompt = "Người nhận: Lan Anh. Xưng: anh. Hô: em. Giọng điệu: SWEET. Dịp: Sinh nhật";
        List<String> milestones = List.of(
                "Đỗ phỏng vấn visa du học (ngày 2024-03-15, danh mục CAREER)",
                "Mua chiếc ô tô đầu tiên (ngày 2024-07-20, danh mục ACHIEVEMENT)"
        );

        String wish = client.generateWish(prompt, milestones, WishLanguage.VI);

        assertNotNull(wish);
        assertFalse(wish.isBlank());
        assertTrue(wish.contains("Lan Anh"), "Wish should mention recipient name");
        assertTrue(wish.toLowerCase().contains("anh"), "Wish should contain pronoun 'anh'");
        assertTrue(wish.toLowerCase().contains("em"), "Wish should contain pronoun 'em'");
        assertTrue(wish.contains("Đỗ phỏng vấn visa du học"), "Should mention first milestone");
        assertTrue(wish.contains("Mua chiếc ô tô đầu tiên"), "Should mention second milestone");
        // Ensure robotic canned greetings are NOT present
        assertFalse(wish.contains("Gửi người bạn tuyệt vời"), "Must not contain robotic boilerplate");
        assertFalse(wish.contains("Luôn ủng hộ và đồng hành cùng bạn!"), "Must not contain robotic closing");
    }

    @Test
    @DisplayName("Should generate playful Vietnamese wish with tớ/cậu pronouns for best friend")
    void testGenerateVietnameseWish_PlayfulTone_ToCau() {
        String prompt = "Người nhận: Tuấn. Xưng: tớ. Hô: cậu. Giọng điệu: PLAYFUL. Dịp: Sinh nhật";
        List<String> milestones = List.of(
                "Chạy hoàn thành cự ly 21km (ngày 2024-05-10, danh mục HEALTH)"
        );

        String wish = client.generateWish(prompt, milestones, WishLanguage.VI);

        assertNotNull(wish);
        assertFalse(wish.isBlank());
        assertTrue(wish.contains("Tuấn"));
        assertTrue(wish.toLowerCase().contains("cậu"));
        assertTrue(wish.contains("Chạy hoàn thành cự ly 21km"));
        assertFalse(wish.contains("Gửi người bạn tuyệt vời"));
    }

    @Test
    @DisplayName("Should generate respectful Vietnamese wish with con/mẹ pronouns")
    void testGenerateVietnameseWish_RespectfulTone_ConMe() {
        String prompt = "Người nhận: Mẹ. Xưng: con. Hô: mẹ. Giọng điệu: RESPECTFUL. Dịp: Tết";
        String wish = client.generateWish(prompt, List.of(), WishLanguage.VI);

        assertNotNull(wish);
        assertFalse(wish.isBlank());
        assertTrue(wish.contains("Mẹ") || wish.contains("mẹ"));
        assertTrue(wish.contains("con"));
        assertTrue(wish.toLowerCase().contains("kính"));
    }

    @Test
    @DisplayName("Should generate English wish with casual tone and proper name")
    void testGenerateEnglishWish_CasualTone() {
        String prompt = "Người nhận: Sarah. Xưng: I. Hô: you. Giọng điệu: CASUAL. Dịp: Birthday";
        List<String> milestones = List.of(
                "Completed 21km half marathon (ngày 2024-05-10, danh mục HEALTH)"
        );

        String wish = client.generateWish(prompt, milestones, WishLanguage.EN);

        assertNotNull(wish);
        assertFalse(wish.isBlank());
        assertTrue(wish.contains("Sarah"));
        assertTrue(wish.contains("Completed 21km half marathon"));
    }

    @Test
    @DisplayName("Should gracefully handle missing pronouns and tone with warm defaults")
    void testGenerateWish_DefaultsWhenFieldsMissing() {
        String prompt = "Người nhận: Minh. Dịp: Sinh nhật";
        String wish = client.generateWish(prompt, List.of(), WishLanguage.VI);

        assertNotNull(wish);
        assertFalse(wish.isBlank());
        assertTrue(wish.contains("Minh"));
    }
}
