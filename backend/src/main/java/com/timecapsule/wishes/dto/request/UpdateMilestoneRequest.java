package com.timecapsule.wishes.dto.request;

import com.timecapsule.wishes.enums.MilestoneCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateMilestoneRequest(
        @NotBlank(message = "Description is required")
        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        @NotNull(message = "Category is required")
        MilestoneCategory category,

        @NotNull(message = "Occurred date is required")
        LocalDate occurredAt
) {
}
