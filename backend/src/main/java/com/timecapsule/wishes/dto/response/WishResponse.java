package com.timecapsule.wishes.dto.response;

import com.timecapsule.wishes.enums.OccasionType;
import com.timecapsule.wishes.enums.WishLanguage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WishResponse(
        UUID id,
        UUID recipientId,
        String recipientName,
        OccasionType occasionType,
        WishLanguage language,
        String generatedText,
        String editedText,
        Integer version,
        List<UUID> milestoneIds,
        Instant createdAt,
        Instant updatedAt
) {
}
