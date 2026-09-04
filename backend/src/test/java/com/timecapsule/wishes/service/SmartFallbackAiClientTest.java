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
    @DisplayName("Should generate rich Vietnamese wish with milestones and personalization")
    void testGenerateVietnameseWish_WithMilestones() {
        String prompt = "Người nhận: Minh Anh (Mối quan hệ: bạn thân). Ghi chú cá nhân: thích du lịch. Dịp: Sinh nhật. Yêu cầu bổ sung: chúc nhiều sức khỏe";
        List<String> milestones = List.of(
                "Đỗ phỏng vấn visa du học Đức (ngày 2024-03-15, danh mục CAREER)",
                "Mua chiếc ô tô đầu tiên (ngày 2024-07-20, danh mục ACHIEVEMENT)"
        );

        String wish = client.generateWish(prompt, milestones, WishLanguage.VI);

        assertNotNull(wish);
        assertFalse(wish.isBlank());
        assertTrue(wish.contains("Minh Anh"));
        assertTrue(wish.contains("Đỗ phỏng vấn visa du học Đức"));
        assertTrue(wish.contains("Mua chiếc ô tô đầu tiên"));
        assertTrue(wish.contains("chúc nhiều sức khỏe"));
    }

    @Test
    @DisplayName("Should generate rich English wish with milestones and personalization")
    void testGenerateEnglishWish_WithMilestones() {
        String prompt = "Người nhận: Sarah (Mối quan hệ: close friend). Dịp: Birthday";
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
    @DisplayName("Should generate warm wish even when zero milestones are provided")
    void testGenerateWish_NoMilestones() {
        String prompt = "Người nhận: Lan. Dịp: Tết";
        String wish = client.generateWish(prompt, List.of(), WishLanguage.VI);

        assertNotNull(wish);
        assertFalse(wish.isBlank());
        assertTrue(wish.contains("Lan"));
    }
}
