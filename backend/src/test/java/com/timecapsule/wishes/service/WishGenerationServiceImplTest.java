package com.timecapsule.wishes.service;

import com.timecapsule.wishes.dto.request.EditWishRequest;
import com.timecapsule.wishes.dto.request.GenerateWishRequest;
import com.timecapsule.wishes.dto.response.WishResponse;
import com.timecapsule.wishes.entity.GeneratedWish;
import com.timecapsule.wishes.entity.Milestone;
import com.timecapsule.wishes.entity.Recipient;
import com.timecapsule.wishes.entity.User;
import com.timecapsule.wishes.enums.MilestoneCategory;
import com.timecapsule.wishes.enums.OccasionType;
import com.timecapsule.wishes.enums.WishLanguage;
import com.timecapsule.wishes.exception.ResourceNotFoundException;
import com.timecapsule.wishes.mapper.WishMapper;
import com.timecapsule.wishes.repository.GeneratedWishRepository;
import com.timecapsule.wishes.repository.MilestoneRepository;
import com.timecapsule.wishes.repository.RecipientRepository;
import com.timecapsule.wishes.security.UserPrincipal;
import com.timecapsule.wishes.service.impl.WishGenerationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishGenerationServiceImplTest {

    @Mock
    private RecipientRepository recipientRepository;

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private GeneratedWishRepository generatedWishRepository;

    @Mock
    private AiClient aiClient;

    @Mock
    private WishMapper wishMapper;

    @InjectMocks
    private WishGenerationServiceImpl wishGenerationService;

    private User sampleUser;
    private UserPrincipal samplePrincipal;
    private Recipient sampleRecipient;
    private Milestone sampleMilestone;
    private GeneratedWish sampleWish;
    private WishResponse sampleResponse;
    private UUID recipientId;
    private UUID milestoneId;
    private UUID wishId;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        sampleUser = User.builder().id(userId).email("user@example.com").displayName("User").build();
        samplePrincipal = UserPrincipal.create(sampleUser);

        recipientId = UUID.randomUUID();
        sampleRecipient = Recipient.builder().id(recipientId).user(sampleUser).name("Mom").build();

        milestoneId = UUID.randomUUID();
        sampleMilestone = Milestone.builder()
                .id(milestoneId)
                .recipient(sampleRecipient)
                .description("Opened her bakery")
                .category(MilestoneCategory.ACHIEVEMENT)
                .occurredAt(LocalDate.of(2025, 1, 15))
                .build();

        wishId = UUID.randomUUID();
        sampleWish = GeneratedWish.builder()
                .id(wishId)
                .recipient(sampleRecipient)
                .occasionType(OccasionType.BIRTHDAY)
                .language(WishLanguage.VI)
                .generatedText("Chúc mừng sinh nhật mẹ yêu! Chúc tiệm bánh luôn đông khách!")
                .editedText(null)
                .version(1)
                .milestones(new HashSet<>(Set.of(sampleMilestone)))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        sampleResponse = new WishResponse(
                wishId,
                recipientId,
                "Mom",
                OccasionType.BIRTHDAY,
                WishLanguage.VI,
                "Chúc mừng sinh nhật mẹ yêu! Chúc tiệm bánh luôn đông khách!",
                null,
                1,
                List.of(milestoneId),
                sampleWish.getCreatedAt(),
                sampleWish.getUpdatedAt()
        );
    }

    @Test
    @DisplayName("Should generate wish personalized with selected milestones and save to database")
    void testGenerateWish_WithMilestones_Success() {
        GenerateWishRequest request = new GenerateWishRequest(
                recipientId,
                List.of(milestoneId),
                OccasionType.BIRTHDAY,
                WishLanguage.VI,
                "Tone ấm áp"
        );

        when(recipientRepository.findByIdAndUserId(recipientId, samplePrincipal.getId()))
                .thenReturn(Optional.of(sampleRecipient));
        when(milestoneRepository.findAllByIdInAndRecipientUserId(List.of(milestoneId), samplePrincipal.getId()))
                .thenReturn(List.of(sampleMilestone));
        when(aiClient.generateWish(anyString(), anyList(), eq(WishLanguage.VI)))
                .thenReturn("Chúc mừng sinh nhật mẹ yêu! Chúc tiệm bánh luôn đông khách!");
        when(generatedWishRepository.save(any(GeneratedWish.class))).thenReturn(sampleWish);
        when(wishMapper.toResponse(sampleWish)).thenReturn(sampleResponse);

        WishResponse response = wishGenerationService.generateWish(request, samplePrincipal);

        assertNotNull(response);
        assertEquals(wishId, response.id());
        assertEquals("Mom", response.recipientName());
        assertEquals(1, response.version());
        verify(generatedWishRepository).save(any(GeneratedWish.class));
    }

    @Test
    @DisplayName("Should generate warm generic fallback wish when zero milestones are provided")
    void testGenerateWish_ZeroMilestones_Success() {
        GenerateWishRequest request = new GenerateWishRequest(
                recipientId,
                List.of(), // empty milestones
                OccasionType.BIRTHDAY,
                WishLanguage.EN,
                null
        );

        GeneratedWish zeroMilestoneWish = GeneratedWish.builder()
                .id(wishId)
                .recipient(sampleRecipient)
                .occasionType(OccasionType.BIRTHDAY)
                .language(WishLanguage.EN)
                .generatedText("Happy birthday Mom! Wishing you a year filled with joy!")
                .version(1)
                .milestones(new HashSet<>())
                .build();

        WishResponse zeroResponse = new WishResponse(
                wishId, recipientId, "Mom", OccasionType.BIRTHDAY, WishLanguage.EN,
                "Happy birthday Mom! Wishing you a year filled with joy!", null, 1, List.of(),
                Instant.now(), Instant.now()
        );

        when(recipientRepository.findByIdAndUserId(recipientId, samplePrincipal.getId()))
                .thenReturn(Optional.of(sampleRecipient));
        when(aiClient.generateWish(anyString(), eq(List.of()), eq(WishLanguage.EN)))
                .thenReturn("Happy birthday Mom! Wishing you a year filled with joy!");
        when(generatedWishRepository.save(any(GeneratedWish.class))).thenReturn(zeroMilestoneWish);
        when(wishMapper.toResponse(zeroMilestoneWish)).thenReturn(zeroResponse);

        WishResponse response = wishGenerationService.generateWish(request, samplePrincipal);

        assertNotNull(response);
        assertEquals(0, response.milestoneIds().size());
        verify(milestoneRepository, never()).findAllByIdInAndRecipientUserId(any(), any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when generating wish for non-owned recipient")
    void testGenerateWish_RecipientNotFound() {
        GenerateWishRequest request = new GenerateWishRequest(
                recipientId, List.of(), OccasionType.BIRTHDAY, WishLanguage.VI, null
        );

        when(recipientRepository.findByIdAndUserId(recipientId, samplePrincipal.getId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> wishGenerationService.generateWish(request, samplePrincipal));
    }

    @Test
    @DisplayName("Should save edited text and increment version counter")
    void testEditWish_Success() {
        EditWishRequest editRequest = new EditWishRequest("Mẹ yêu, chúc tiệm bánh ngày càng phát đạt hơn nữa!");

        when(generatedWishRepository.findByIdAndRecipientUserId(wishId, samplePrincipal.getId()))
                .thenReturn(Optional.of(sampleWish));
        when(generatedWishRepository.save(sampleWish)).thenReturn(sampleWish);

        WishResponse editedResponse = new WishResponse(
                wishId, recipientId, "Mom", OccasionType.BIRTHDAY, WishLanguage.VI,
                sampleWish.getGeneratedText(),
                "Mẹ yêu, chúc tiệm bánh ngày càng phát đạt hơn nữa!",
                2,
                List.of(milestoneId),
                sampleWish.getCreatedAt(),
                sampleWish.getUpdatedAt()
        );
        when(wishMapper.toResponse(sampleWish)).thenReturn(editedResponse);

        WishResponse response = wishGenerationService.editWish(wishId, editRequest, samplePrincipal);

        assertNotNull(response);
        assertEquals(2, response.version());
        assertEquals("Mẹ yêu, chúc tiệm bánh ngày càng phát đạt hơn nữa!", response.editedText());
        verify(generatedWishRepository).save(sampleWish);
    }

    @Test
    @DisplayName("Should return wish history for recipient when owned by user")
    void testGetWishesByRecipient_Success() {
        when(recipientRepository.findByIdAndUserId(recipientId, samplePrincipal.getId()))
                .thenReturn(Optional.of(sampleRecipient));
        when(generatedWishRepository.findAllByRecipientIdAndRecipientUserIdOrderByCreatedAtDesc(recipientId, samplePrincipal.getId()))
                .thenReturn(List.of(sampleWish));
        when(wishMapper.toResponseList(List.of(sampleWish))).thenReturn(List.of(sampleResponse));

        List<WishResponse> wishes = wishGenerationService.getWishesByRecipient(recipientId, samplePrincipal);

        assertEquals(1, wishes.size());
        assertEquals(wishId, wishes.get(0).id());
    }
}
