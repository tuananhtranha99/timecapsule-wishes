package com.timecapsule.wishes.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateRecipientRequest(
        @NotBlank(message = "Recipient name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        LocalDate birthday,

        @Size(max = 100, message = "Relationship must not exceed 100 characters")
        String relationship,

        @Size(max = 1000, message = "Notes must not exceed 1000 characters")
        String notes
) {
}
