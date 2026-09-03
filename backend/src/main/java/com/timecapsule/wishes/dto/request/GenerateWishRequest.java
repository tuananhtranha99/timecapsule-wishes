package com.timecapsule.wishes.dto.request;

import com.timecapsule.wishes.enums.OccasionType;
import com.timecapsule.wishes.enums.WishLanguage;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record GenerateWishRequest(
        @NotNull(message = "Recipient ID is required")
        UUID recipientId,

        List<UUID> milestoneIds,

        @NotNull(message = "Occasion type is required")
        OccasionType occasionType,

        @NotNull(message = "Language is required")
        WishLanguage language,

        String customPrompt
) {
}
