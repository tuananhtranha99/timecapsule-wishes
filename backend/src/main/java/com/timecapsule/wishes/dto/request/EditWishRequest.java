package com.timecapsule.wishes.dto.request;

import jakarta.validation.constraints.NotBlank;

public record EditWishRequest(
        @NotBlank(message = "Edited wish text is required")
        String editedText
) {
}
