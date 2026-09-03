package com.timecapsule.wishes.service.impl;

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
import com.timecapsule.wishes.service.RecipientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipientServiceImpl implements RecipientService {

    private final RecipientRepository recipientRepository;
    private final UserRepository userRepository;
    private final RecipientMapper recipientMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RecipientResponse> getAllRecipients(UserPrincipal principal) {
        List<Recipient> recipients = recipientRepository.findAllByUserIdOrderByCreatedAtDesc(principal.getId());
        return recipientMapper.toResponseList(recipients);
    }

    @Override
    @Transactional(readOnly = true)
    public RecipientResponse getRecipientById(UUID id, UserPrincipal principal) {
        Recipient recipient = recipientRepository.findByIdAndUserId(id, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient", "id", id));
        return recipientMapper.toResponse(recipient);
    }

    @Override
    @Transactional
    public RecipientResponse createRecipient(CreateRecipientRequest request, UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        Recipient recipient = recipientMapper.toEntity(request);
        recipient.setUser(user);

        Recipient saved = recipientRepository.save(recipient);
        log.info("Created recipient '{}' (id: {}) for user: {}", saved.getName(), saved.getId(), principal.getId());

        return recipientMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RecipientResponse updateRecipient(UUID id, UpdateRecipientRequest request, UserPrincipal principal) {
        Recipient recipient = recipientRepository.findByIdAndUserId(id, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient", "id", id));

        recipientMapper.updateEntityFromRequest(request, recipient);
        Recipient updated = recipientRepository.save(recipient);
        log.info("Updated recipient id: {} for user: {}", updated.getId(), principal.getId());

        return recipientMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteRecipient(UUID id, UserPrincipal principal) {
        Recipient recipient = recipientRepository.findByIdAndUserId(id, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient", "id", id));

        recipientRepository.delete(recipient);
        log.info("Deleted recipient id: {} for user: {}", id, principal.getId());
    }
}
