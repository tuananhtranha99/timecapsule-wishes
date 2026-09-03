package com.timecapsule.wishes.dto.response;

import com.timecapsule.wishes.enums.MilestoneCategory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MilestoneResponse(
        UUID id,
        UUID recipientId,
        String description,
        MilestoneCategory category,
        LocalDate occurredAt,
        Instant createdAt,
        Instant updatedAt
) {
}
