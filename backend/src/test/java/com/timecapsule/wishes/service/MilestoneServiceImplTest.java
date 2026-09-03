package com.timecapsule.wishes.service;

import com.timecapsule.wishes.dto.request.CreateMilestoneRequest;
import com.timecapsule.wishes.dto.request.UpdateMilestoneRequest;
import com.timecapsule.wishes.dto.response.MilestoneResponse;
import com.timecapsule.wishes.entity.Milestone;
import com.timecapsule.wishes.entity.Recipient;
import com.timecapsule.wishes.entity.User;
import com.timecapsule.wishes.enums.MilestoneCategory;
import com.timecapsule.wishes.exception.ResourceNotFoundException;
import com.timecapsule.wishes.mapper.MilestoneMapper;
import com.timecapsule.wishes.repository.MilestoneRepository;
import com.timecapsule.wishes.repository.RecipientRepository;
import com.timecapsule.wishes.security.UserPrincipal;
import com.timecapsule.wishes.service.impl.MilestoneServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MilestoneServiceImplTest {

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private RecipientRepository recipientRepository;

    @Mock
    private MilestoneMapper milestoneMapper;

    @InjectMocks
    private MilestoneServiceImpl milestoneService;

    private User sampleUser;
    private UserPrincipal samplePrincipal;
    private Recipient sampleRecipient;
    private Milestone sampleMilestone;
    private MilestoneResponse sampleResponse;
    private UUID recipientId;
    private UUID milestoneId;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(userId)
                .email("user@example.com")
                .displayName("User")
                .build();
        samplePrincipal = UserPrincipal.create(sampleUser);

        recipientId = UUID.randomUUID();
        sampleRecipient = Recipient.builder()
                .id(recipientId)
                .user(sampleUser)
                .name("Jane Doe")
                .build();

        milestoneId = UUID.randomUUID();
        sampleMilestone = Milestone.builder()
                .id(milestoneId)
                .recipient(sampleRecipient)
                .description("Passed visa interview")
                .category(MilestoneCategory.CAREER)
                .occurredAt(LocalDate.of(2025, 3, 10))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        sampleResponse = new MilestoneResponse(
                milestoneId,
                recipientId,
                "Passed visa interview",
                MilestoneCategory.CAREER,
                LocalDate.of(2025, 3, 10),
                sampleMilestone.getCreatedAt(),
                sampleMilestone.getUpdatedAt()
        );
    }

    @Test
    @DisplayName("Should return milestones for recipient when owned by user")
    void testGetMilestonesByRecipient_Success() {
        when(recipientRepository.findByIdAndUserId(recipientId, samplePrincipal.getId()))
                .thenReturn(Optional.of(sampleRecipient));
        when(milestoneRepository.findAllByRecipientIdAndRecipientUserIdOrderByOccurredAtDesc(recipientId, samplePrincipal.getId()))
                .thenReturn(List.of(sampleMilestone));
        when(milestoneMapper.toResponseList(List.of(sampleMilestone)))
                .thenReturn(List.of(sampleResponse));

        List<MilestoneResponse> results = milestoneService.getMilestonesByRecipient(recipientId, samplePrincipal);

        assertEquals(1, results.size());
        assertEquals("Passed visa interview", results.get(0).description());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when recipient does not belong to user on get milestones")
    void testGetMilestonesByRecipient_RecipientNotFound() {
        when(recipientRepository.findByIdAndUserId(recipientId, samplePrincipal.getId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> milestoneService.getMilestonesByRecipient(recipientId, samplePrincipal));
    }

    @Test
    @DisplayName("Should successfully create a milestone with backdated occurredAt")
    void testCreateMilestone_Success() {
        CreateMilestoneRequest request = new CreateMilestoneRequest(
                "Bought a new car",
                MilestoneCategory.ACHIEVEMENT,
                LocalDate.of(2024, 11, 1) // backdated
        );

        when(recipientRepository.findByIdAndUserId(recipientId, samplePrincipal.getId()))
                .thenReturn(Optional.of(sampleRecipient));
        when(milestoneMapper.toEntity(request)).thenReturn(sampleMilestone);
        when(milestoneRepository.save(any(Milestone.class))).thenReturn(sampleMilestone);
        when(milestoneMapper.toResponse(sampleMilestone)).thenReturn(sampleResponse);

        MilestoneResponse result = milestoneService.createMilestone(recipientId, request, samplePrincipal);

        assertNotNull(result);
        assertEquals("Passed visa interview", result.description());
        verify(milestoneRepository).save(sampleMilestone);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when creating milestone for non-owned recipient")
    void testCreateMilestone_RecipientNotFound() {
        CreateMilestoneRequest request = new CreateMilestoneRequest(
                "Test",
                MilestoneCategory.OTHER,
                LocalDate.now()
        );

        when(recipientRepository.findByIdAndUserId(recipientId, samplePrincipal.getId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> milestoneService.createMilestone(recipientId, request, samplePrincipal));
        verify(milestoneRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update milestone successfully when owned by user")
    void testUpdateMilestone_Success() {
        UpdateMilestoneRequest request = new UpdateMilestoneRequest(
                "Passed visa interview with distinction",
                MilestoneCategory.CAREER,
                LocalDate.of(2025, 3, 10)
        );

        when(milestoneRepository.findByIdAndRecipientUserId(milestoneId, samplePrincipal.getId()))
                .thenReturn(Optional.of(sampleMilestone));
        doAnswer(inv -> {
            sampleMilestone.setDescription("Passed visa interview with distinction");
            return null;
        }).when(milestoneMapper).updateEntityFromRequest(request, sampleMilestone);
        when(milestoneRepository.save(sampleMilestone)).thenReturn(sampleMilestone);

        MilestoneResponse updatedResponse = new MilestoneResponse(
                milestoneId,
                recipientId,
                "Passed visa interview with distinction",
                MilestoneCategory.CAREER,
                LocalDate.of(2025, 3, 10),
                sampleMilestone.getCreatedAt(),
                sampleMilestone.getUpdatedAt()
        );
        when(milestoneMapper.toResponse(sampleMilestone)).thenReturn(updatedResponse);

        MilestoneResponse result = milestoneService.updateMilestone(milestoneId, request, samplePrincipal);

        assertNotNull(result);
        assertEquals("Passed visa interview with distinction", result.description());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent or non-owned milestone")
    void testUpdateMilestone_NotFound() {
        UpdateMilestoneRequest request = new UpdateMilestoneRequest("Test", MilestoneCategory.OTHER, LocalDate.now());

        when(milestoneRepository.findByIdAndRecipientUserId(milestoneId, samplePrincipal.getId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> milestoneService.updateMilestone(milestoneId, request, samplePrincipal));
    }

    @Test
    @DisplayName("Should delete milestone when owned by user")
    void testDeleteMilestone_Success() {
        when(milestoneRepository.findByIdAndRecipientUserId(milestoneId, samplePrincipal.getId()))
                .thenReturn(Optional.of(sampleMilestone));

        assertDoesNotThrow(() -> milestoneService.deleteMilestone(milestoneId, samplePrincipal));

        verify(milestoneRepository).delete(sampleMilestone);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent or non-owned milestone")
    void testDeleteMilestone_NotFound() {
        when(milestoneRepository.findByIdAndRecipientUserId(milestoneId, samplePrincipal.getId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> milestoneService.deleteMilestone(milestoneId, samplePrincipal));
        verify(milestoneRepository, never()).delete(any());
    }
}
