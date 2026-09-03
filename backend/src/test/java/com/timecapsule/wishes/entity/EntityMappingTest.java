package com.timecapsule.wishes.entity;

import com.timecapsule.wishes.enums.MilestoneCategory;
import com.timecapsule.wishes.enums.OccasionType;
import com.timecapsule.wishes.enums.WishLanguage;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class EntityMappingTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Should persist and query User, Recipient, Milestone, and GeneratedWish with valid mappings")
    void testEntityLifecycleAndRelationships() {
        // 1. Create and persist User
        User user = User.builder()
                .email("test.wisher@example.com")
                .passwordHash("$2a$10$encryptedPasswordHashHere")
                .displayName("Nguyen Van A")
                .build();
        entityManager.persist(user);
        entityManager.flush();
        assertThat(user.getId()).isNotNull();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();

        // 2. Create and persist Recipient
        Recipient recipient = Recipient.builder()
                .user(user)
                .name("Tran Thi B")
                .birthday(LocalDate.of(1995, 8, 15))
                .relationship("Best Friend")
                .notes("Loves running and reading")
                .build();
        entityManager.persist(recipient);
        entityManager.flush();
        assertThat(recipient.getId()).isNotNull();
        assertThat(recipient.getUser().getId()).isEqualTo(user.getId());

        // 3. Create and persist Milestone
        Milestone milestone = Milestone.builder()
                .recipient(recipient)
                .description("Completed her first half-marathon in Da Nang")
                .category(MilestoneCategory.ACHIEVEMENT)
                .occurredAt(LocalDate.of(2025, 4, 10))
                .build();
        entityManager.persist(milestone);
        entityManager.flush();
        assertThat(milestone.getId()).isNotNull();
        assertThat(milestone.getCategory()).isEqualTo(MilestoneCategory.ACHIEVEMENT);

        // 4. Create and persist GeneratedWish linked to Milestone
        GeneratedWish wish = GeneratedWish.builder()
                .recipient(recipient)
                .occasionType(OccasionType.BIRTHDAY)
                .language(WishLanguage.VI)
                .generatedText("Chúc mừng sinh nhật B! Nhớ đợt hoàn thành half-marathon ở Đà Nẵng...")
                .version(1)
                .milestones(Set.of(milestone))
                .build();
        entityManager.persist(wish);
        entityManager.flush();
        assertThat(wish.getId()).isNotNull();
        assertThat(wish.getMilestones()).hasSize(1);
        assertThat(wish.getVersion()).isEqualTo(1);

        // 5. Clear persistence context and reload from DB to verify mapping and relations
        entityManager.clear();

        GeneratedWish reloadedWish = entityManager.find(GeneratedWish.class, wish.getId());
        assertThat(reloadedWish).isNotNull();
        assertThat(reloadedWish.getGeneratedText()).isEqualTo(wish.getGeneratedText());
        assertThat(reloadedWish.getOccasionType()).isEqualTo(OccasionType.BIRTHDAY);
        assertThat(reloadedWish.getLanguage()).isEqualTo(WishLanguage.VI);
        assertThat(reloadedWish.getRecipient().getName()).isEqualTo("Tran Thi B");
        assertThat(reloadedWish.getRecipient().getUser().getEmail()).isEqualTo("test.wisher@example.com");
        assertThat(reloadedWish.getMilestones()).hasSize(1);
        Milestone reloadedMilestone = reloadedWish.getMilestones().iterator().next();
        assertThat(reloadedMilestone.getDescription()).contains("half-marathon");
    }
}
