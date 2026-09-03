package com.timecapsule.wishes.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        boolean success,
        int status,
        String error,
        String message,
        Map<String, String> validationErrors,
        Instant timestamp
) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(false, status, error, message, null, Instant.now());
    }

    public static ErrorResponse of(int status, String error, String message, Map<String, String> validationErrors) {
        return new ErrorResponse(false, status, error, message, validationErrors, Instant.now());
    }
}
