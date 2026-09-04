package com.timecapsule.wishes.service.impl;

import com.timecapsule.wishes.dto.request.EditWishRequest;
import com.timecapsule.wishes.dto.request.GenerateWishRequest;
import com.timecapsule.wishes.dto.response.WishResponse;
import com.timecapsule.wishes.entity.GeneratedWish;
import com.timecapsule.wishes.entity.Milestone;
import com.timecapsule.wishes.entity.Recipient;
import com.timecapsule.wishes.exception.ResourceNotFoundException;
import com.timecapsule.wishes.mapper.WishMapper;
import com.timecapsule.wishes.repository.GeneratedWishRepository;
import com.timecapsule.wishes.repository.MilestoneRepository;
import com.timecapsule.wishes.repository.RecipientRepository;
import com.timecapsule.wishes.security.UserPrincipal;
import com.timecapsule.wishes.service.AiClient;
import com.timecapsule.wishes.service.WishGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishGenerationServiceImpl implements WishGenerationService {

    private final RecipientRepository recipientRepository;
    private final MilestoneRepository milestoneRepository;
    private final GeneratedWishRepository generatedWishRepository;
    private final AiClient aiClient;
    private final WishMapper wishMapper;

    @Override
    @Transactional
    public WishResponse generateWish(GenerateWishRequest request, UserPrincipal principal) {
        Recipient recipient = recipientRepository.findByIdAndUserId(request.recipientId(), principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient", "id", request.recipientId()));

        List<Milestone> selectedMilestones = List.of();
        if (request.milestoneIds() != null && !request.milestoneIds().isEmpty()) {
            selectedMilestones = milestoneRepository.findAllByIdInAndRecipientUserId(request.milestoneIds(), principal.getId())
                    .stream()
                    .filter(m -> m.getRecipient().getId().equals(recipient.getId()))
                    .toList();
        }

        List<String> milestoneDescriptions = selectedMilestones.stream()
                .map(m -> String.format("%s (ngày %s, danh mục %s)",
                        m.getDescription(), m.getOccurredAt(), m.getCategory()))
                .toList();

        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Người nhận: ").append(recipient.getName());
        if (request.pronounSelf() != null && !request.pronounSelf().isBlank()) {
            contextBuilder.append(". Xưng: ").append(request.pronounSelf().trim());
        }
        if (request.pronounRecipient() != null && !request.pronounRecipient().isBlank()) {
            contextBuilder.append(". Hô: ").append(request.pronounRecipient().trim());
        }
        if (request.toneStyle() != null) {
            contextBuilder.append(". Giọng điệu: ").append(request.toneStyle().name());
        }
        if (recipient.getNotes() != null && !recipient.getNotes().isBlank()) {
            contextBuilder.append(". Ghi chú cá nhân: ").append(recipient.getNotes());
        }
        contextBuilder.append(". Dịp: ").append(request.occasionType());
        if (request.customPrompt() != null && !request.customPrompt().isBlank()) {
            contextBuilder.append(". Yêu cầu bổ sung: ").append(request.customPrompt());
        }

        log.info("Generating wish for recipient {} with {} milestones using AI...",
                recipient.getId(), milestoneDescriptions.size());

        String generatedText = aiClient.generateWish(contextBuilder.toString(), milestoneDescriptions, request.language());

        GeneratedWish wish = GeneratedWish.builder()
                .recipient(recipient)
                .occasionType(request.occasionType())
                .language(request.language())
                .generatedText(generatedText)
                .editedText(null)
                .version(1)
                .milestones(new HashSet<>(selectedMilestones))
                .build();

        GeneratedWish saved = generatedWishRepository.save(wish);
        log.info("Generated wish saved with id: {} for recipient: {}", saved.getId(), recipient.getId());

        return wishMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishResponse> getWishesByRecipient(UUID recipientId, UserPrincipal principal) {
        recipientRepository.findByIdAndUserId(recipientId, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient", "id", recipientId));

        List<GeneratedWish> wishes = generatedWishRepository
                .findAllByRecipientIdAndRecipientUserIdOrderByCreatedAtDesc(recipientId, principal.getId());
        return wishMapper.toResponseList(wishes);
    }

    @Override
    @Transactional(readOnly = true)
    public WishResponse getWishById(UUID id, UserPrincipal principal) {
        GeneratedWish wish = generatedWishRepository.findByIdAndRecipientUserId(id, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("GeneratedWish", "id", id));
        return wishMapper.toResponse(wish);
    }

    @Override
    @Transactional
    public WishResponse editWish(UUID id, EditWishRequest request, UserPrincipal principal) {
        GeneratedWish wish = generatedWishRepository.findByIdAndRecipientUserId(id, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("GeneratedWish", "id", id));

        wish.setEditedText(request.editedText().trim());
        wish.setVersion(wish.getVersion() + 1);

        GeneratedWish updated = generatedWishRepository.save(wish);
        log.info("Wish id: {} updated to version: {}", updated.getId(), updated.getVersion());

        return wishMapper.toResponse(updated);
    }
}
