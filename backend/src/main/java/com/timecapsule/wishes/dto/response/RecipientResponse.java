package com.timecapsule.wishes.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RecipientResponse(
        UUID id,
        String name,
        LocalDate birthday,
        String relationship,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
