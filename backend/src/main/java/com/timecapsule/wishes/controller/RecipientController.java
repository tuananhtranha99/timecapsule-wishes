package com.timecapsule.wishes.controller;

import com.timecapsule.wishes.dto.request.CreateRecipientRequest;
import com.timecapsule.wishes.dto.request.UpdateRecipientRequest;
import com.timecapsule.wishes.dto.response.ApiResponse;
import com.timecapsule.wishes.dto.response.RecipientResponse;
import com.timecapsule.wishes.security.UserPrincipal;
import com.timecapsule.wishes.service.RecipientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/recipients", "/recipients"})
@RequiredArgsConstructor
@Tag(name = "Recipients", description = "CRUD operations for people you care about")
public class RecipientController {

    private final RecipientService recipientService;

    @Operation(summary = "List all recipients for current user")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RecipientResponse>>> getAllRecipients(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<RecipientResponse> recipients = recipientService.getAllRecipients(principal);
        return ResponseEntity.ok(ApiResponse.success(recipients));
    }

    @Operation(summary = "Get single recipient details by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecipientResponse>> getRecipientById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        RecipientResponse recipient = recipientService.getRecipientById(id, principal);
        return ResponseEntity.ok(ApiResponse.success(recipient));
    }

    @Operation(summary = "Create a new recipient")
    @PostMapping
    public ResponseEntity<ApiResponse<RecipientResponse>> createRecipient(
            @Valid @RequestBody CreateRecipientRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        RecipientResponse created = recipientService.createRecipient(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Recipient created successfully", created));
    }

    @Operation(summary = "Update an existing recipient")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RecipientResponse>> updateRecipient(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRecipientRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        RecipientResponse updated = recipientService.updateRecipient(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success("Recipient updated successfully", updated));
    }

    @Operation(summary = "Delete recipient along with all associated milestones and wishes")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRecipient(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        recipientService.deleteRecipient(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Recipient deleted successfully", null));
    }
}
