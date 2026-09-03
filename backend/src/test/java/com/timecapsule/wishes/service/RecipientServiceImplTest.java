package com.timecapsule.wishes.service;

import com.timecapsule.wishes.dto.request.CreateRecipientRequest;
import com.timecapsule.wishes.dto.request.UpdateRecipientRequest;
import com.timecapsule.wishes.dto.response.RecipientResponse;
import com.timecapsule.wishes.entity.Recipient;
import com.timecapsule.wishes.entity.User;
import com.timecapsule.wishes.exception.ResourceNotFoundException;
import com.timecapsule.wishes.mapper.RecipientMapper;
import com.timecapsule.wishes.repository.RecipientRepository;
import com.timecapsule.wishes.repository.UserRepository;
import com.timecapsule.wishes.security.UserPrincipal;
import com.timecapsule.wishes.service.impl.RecipientServiceImpl;
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
class RecipientServiceImplTest {

    @Mock
    private RecipientRepository recipientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecipientMapper recipientMapper;

    @InjectMocks
    private RecipientServiceImpl recipientService;

    private User sampleUser;
    private UserPrincipal samplePrincipal;
    private Recipient sampleRecipient;
    private RecipientResponse sampleResponse;
    private UUID recipientId;

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
                .birthday(LocalDate.of(1995, 5, 20))
                .relationship("Friend")
                .notes("Loves coffee")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        sampleResponse = new RecipientResponse(
                recipientId,
                "Jane Doe",
                LocalDate.of(1995, 5, 20),
                "Friend",
                "Loves coffee",
                sampleRecipient.getCreatedAt(),
                sampleRecipient.getUpdatedAt()
        );
    }

    @Test
    @DisplayName("Should return all recipients for the authenticated user")
    void testGetAllRecipients_Success() {
        when(recipientRepository.findAllByUserIdOrderByCreatedAtDesc(samplePrincipal.getId()))
                .thenReturn(List.of(sampleRecipient));
        when(recipientMapper.toResponseList(List.of(sampleRecipient)))
                .thenReturn(List.of(sampleResponse));

        List<RecipientResponse> results = recipientService.getAllRecipients(samplePrincipal);

        assertEquals(1, results.size());
        assertEquals("Jane Doe", results.get(0).name());
        verify(recipientRepository).findAllByUserIdOrderByCreatedAtDesc(samplePrincipal.getId());
    }

    @Test
    @DisplayName("Should return recipient by id when owned by authenticated user")
    void testGetRecipientById_Success() {
        when(recipientRepository.findByIdAndUserId(recipientId, samplePrincipal.getId()))
                .thenReturn(Optional.of(sampleRecipient));
        when(recipientMapper.toResponse(sampleRecipient)).thenReturn(sampleResponse);

        RecipientResponse result = recipientService.getRecipientById(recipientId, samplePrincipal);

        assertNotNull(result);
        assertEquals(recipientId, result.id());
        assertEquals("Jane Doe", result.name());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when recipient not found for user")
    void testGetRecipientById_NotFound() {
        when(recipientRepository.findByIdAndUserId(recipientId, samplePrincipal.getId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> recipientService.getRecipientById(recipientId, samplePrincipal));
    }

    @Test
    @DisplayName("Should successfully create a new recipient for the authenticated user")
    void testCreateRecipient_Success() {
        CreateRecipientRequest request = new CreateRecipientRequest(
                "Jane Doe",
                LocalDate.of(1995, 5, 20),
                "Friend",
                "Loves coffee"
        );

        when(userRepository.findById(samplePrincipal.getId())).thenReturn(Optional.of(sampleUser));
        when(recipientMapper.toEntity(request)).thenReturn(sampleRecipient);
        when(recipientRepository.save(any(Recipient.class))).thenReturn(sampleRecipient);
        when(recipientMapper.toResponse(sampleRecipient)).thenReturn(sampleResponse);

        RecipientResponse result = recipientService.createRecipient(request, samplePrincipal);

        assertNotNull(result);
        assertEquals("Jane Doe", result.name());
        verify(recipientRepository).save(sampleRecipient);
    }

    @Test
    @DisplayName("Should update recipient fields when owned by authenticated user")
    void testUpdateRecipient_Success() {
        UpdateRecipientRequest request = new UpdateRecipientRequest(
                "Jane Smith",
                LocalDate.of(1995, 5, 20),
                "Best Friend",
                "Updated notes"
        );

        when(recipientRepository.findByIdAndUserId(recipientId, samplePrincipal.getId()))
                .thenReturn(Optional.of(sampleRecipient));
        doAnswer(invocation -> {
            sampleRecipient.setName("Jane Smith");
            sampleRecipient.setRelationship("Best Friend");
            return null;
        }).when(recipientMapper).updateEntityFromRequest(request, sampleRecipient);
        when(recipientRepository.save(sampleRecipient)).thenReturn(sampleRecipient);

        RecipientResponse updatedResponse = new RecipientResponse(
                recipientId,
                "Jane Smith",
                LocalDate.of(1995, 5, 20),
                "Best Friend",
                "Updated notes",
                sampleRecipient.getCreatedAt(),
                sampleRecipient.getUpdatedAt()
        );
        when(recipientMapper.toResponse(sampleRecipient)).thenReturn(updatedResponse);

        RecipientResponse result = recipientService.updateRecipient(recipientId, request, samplePrincipal);

        assertNotNull(result);
        assertEquals("Jane Smith", result.name());
        assertEquals("Best Friend", result.relationship());
    }

    @Test
    @DisplayName("Should delete recipient when owned by authenticated user")
    void testDeleteRecipient_Success() {
        when(recipientRepository.findByIdAndUserId(recipientId, samplePrincipal.getId()))
                .thenReturn(Optional.of(sampleRecipient));

        assertDoesNotThrow(() -> recipientService.deleteRecipient(recipientId, samplePrincipal));

        verify(recipientRepository).delete(sampleRecipient);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when trying to delete another user's recipient")
    void testDeleteRecipient_NotFound() {
        when(recipientRepository.findByIdAndUserId(recipientId, samplePrincipal.getId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> recipientService.deleteRecipient(recipientId, samplePrincipal));
        verify(recipientRepository, never()).delete(any());
    }
}
