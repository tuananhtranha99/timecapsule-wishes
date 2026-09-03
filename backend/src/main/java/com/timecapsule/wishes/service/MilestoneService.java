package com.timecapsule.wishes.service;

import com.timecapsule.wishes.dto.request.CreateMilestoneRequest;
import com.timecapsule.wishes.dto.request.UpdateMilestoneRequest;
import com.timecapsule.wishes.dto.response.MilestoneResponse;
import com.timecapsule.wishes.security.UserPrincipal;

import java.util.List;
import java.util.UUID;

public interface MilestoneService {

    List<MilestoneResponse> getMilestonesByRecipient(UUID recipientId, UserPrincipal principal);

    MilestoneResponse getMilestoneById(UUID id, UserPrincipal principal);

    MilestoneResponse createMilestone(UUID recipientId, CreateMilestoneRequest request, UserPrincipal principal);

    MilestoneResponse updateMilestone(UUID id, UpdateMilestoneRequest request, UserPrincipal principal);

    void deleteMilestone(UUID id, UserPrincipal principal);
}
