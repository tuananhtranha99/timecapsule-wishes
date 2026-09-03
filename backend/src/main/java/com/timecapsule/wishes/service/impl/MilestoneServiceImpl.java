package com.timecapsule.wishes.service.impl;

import com.timecapsule.wishes.dto.request.CreateMilestoneRequest;
import com.timecapsule.wishes.dto.request.UpdateMilestoneRequest;
import com.timecapsule.wishes.dto.response.MilestoneResponse;
import com.timecapsule.wishes.entity.Milestone;
import com.timecapsule.wishes.entity.Recipient;
import com.timecapsule.wishes.exception.ResourceNotFoundException;
import com.timecapsule.wishes.mapper.MilestoneMapper;
import com.timecapsule.wishes.repository.MilestoneRepository;
import com.timecapsule.wishes.repository.RecipientRepository;
import com.timecapsule.wishes.security.UserPrincipal;
import com.timecapsule.wishes.service.MilestoneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MilestoneServiceImpl implements MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final RecipientRepository recipientRepository;
    private final MilestoneMapper milestoneMapper;

    @Override
    @Transactional(readOnly = true)
    public List<MilestoneResponse> getMilestonesByRecipient(UUID recipientId, UserPrincipal principal) {
        recipientRepository.findByIdAndUserId(recipientId, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient", "id", recipientId));

        List<Milestone> milestones = milestoneRepository
                .findAllByRecipientIdAndRecipientUserIdOrderByOccurredAtDesc(recipientId, principal.getId());
        return milestoneMapper.toResponseList(milestones);
    }

    @Override
    @Transactional(readOnly = true)
    public MilestoneResponse getMilestoneById(UUID id, UserPrincipal principal) {
        Milestone milestone = milestoneRepository.findByIdAndRecipientUserId(id, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", "id", id));
        return milestoneMapper.toResponse(milestone);
    }

    @Override
    @Transactional
    public MilestoneResponse createMilestone(UUID recipientId, CreateMilestoneRequest request, UserPrincipal principal) {
        Recipient recipient = recipientRepository.findByIdAndUserId(recipientId, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient", "id", recipientId));

        Milestone milestone = milestoneMapper.toEntity(request);
        milestone.setRecipient(recipient);

        Milestone saved = milestoneRepository.save(milestone);
        log.info("Created milestone '{}' (id: {}) for recipient: {} by user: {}",
                saved.getDescription(), saved.getId(), recipientId, principal.getId());

        return milestoneMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public MilestoneResponse updateMilestone(UUID id, UpdateMilestoneRequest request, UserPrincipal principal) {
        Milestone milestone = milestoneRepository.findByIdAndRecipientUserId(id, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", "id", id));

        milestoneMapper.updateEntityFromRequest(request, milestone);
        Milestone updated = milestoneRepository.save(milestone);
        log.info("Updated milestone id: {} by user: {}", updated.getId(), principal.getId());

        return milestoneMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteMilestone(UUID id, UserPrincipal principal) {
        Milestone milestone = milestoneRepository.findByIdAndRecipientUserId(id, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", "id", id));

        milestoneRepository.delete(milestone);
        log.info("Deleted milestone id: {} by user: {}", id, principal.getId());
    }
}
