package com.timecapsule.wishes.controller;

import com.timecapsule.wishes.dto.request.EditWishRequest;
import com.timecapsule.wishes.dto.request.GenerateWishRequest;
import com.timecapsule.wishes.dto.response.ApiResponse;
import com.timecapsule.wishes.dto.response.WishResponse;
import com.timecapsule.wishes.security.UserPrincipal;
import com.timecapsule.wishes.service.WishGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Wishes", description = "AI wish generation, history, and revision tracking")
public class WishController {

    private final WishGenerationService wishGenerationService;

    @Operation(summary = "Generate a personalized wish using AI synthesizing selected milestones")
    @PostMapping({"/api/v1/wishes/generate", "/wishes/generate"})
    public ResponseEntity<ApiResponse<WishResponse>> generateWish(
            @Valid @RequestBody GenerateWishRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        WishResponse response = wishGenerationService.generateWish(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Wish generated successfully", response));
    }

    @Operation(summary = "Get wish generation history for a recipient")
    @GetMapping({"/api/v1/recipients/{recipientId}/wishes", "/recipients/{recipientId}/wishes"})
    public ResponseEntity<ApiResponse<List<WishResponse>>> getWishesByRecipient(
            @PathVariable UUID recipientId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<WishResponse> wishes = wishGenerationService.getWishesByRecipient(recipientId, principal);
        return ResponseEntity.ok(ApiResponse.success(wishes));
    }

    @Operation(summary = "Get single wish by ID")
    @GetMapping({"/api/v1/wishes/{id}", "/wishes/{id}"})
    public ResponseEntity<ApiResponse<WishResponse>> getWishById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        WishResponse wish = wishGenerationService.getWishById(id, principal);
        return ResponseEntity.ok(ApiResponse.success(wish));
    }

    @Operation(summary = "Edit and save a new revision of a wish")
    @PutMapping({"/api/v1/wishes/{id}", "/wishes/{id}"})
    public ResponseEntity<ApiResponse<WishResponse>> editWish(
            @PathVariable UUID id,
            @Valid @RequestBody EditWishRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        WishResponse updated = wishGenerationService.editWish(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success("Wish edited successfully", updated));
    }
}
