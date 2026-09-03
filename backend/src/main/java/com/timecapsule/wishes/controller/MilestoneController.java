package com.timecapsule.wishes.controller;

import com.timecapsule.wishes.dto.request.CreateMilestoneRequest;
import com.timecapsule.wishes.dto.request.UpdateMilestoneRequest;
import com.timecapsule.wishes.dto.response.ApiResponse;
import com.timecapsule.wishes.dto.response.MilestoneResponse;
import com.timecapsule.wishes.security.UserPrincipal;
import com.timecapsule.wishes.service.MilestoneService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Milestones", description = "Logging and tracking personal milestones for recipients")
public class MilestoneController {

    private final MilestoneService milestoneService;

    @Operation(summary = "Get all milestones for a recipient ordered chronologically")
    @GetMapping("/api/v1/recipients/{recipientId}/milestones")
    public ResponseEntity<ApiResponse<List<MilestoneResponse>>> getMilestonesByRecipient(
            @PathVariable UUID recipientId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<MilestoneResponse> milestones = milestoneService.getMilestonesByRecipient(recipientId, principal);
        return ResponseEntity.ok(ApiResponse.success(milestones));
    }

    @Operation(summary = "Log a new milestone for a recipient")
    @PostMapping("/api/v1/recipients/{recipientId}/milestones")
    public ResponseEntity<ApiResponse<MilestoneResponse>> createMilestone(
            @PathVariable UUID recipientId,
            @Valid @RequestBody CreateMilestoneRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        MilestoneResponse created = milestoneService.createMilestone(recipientId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Milestone created successfully", created));
    }

    @Operation(summary = "Get a single milestone by ID")
    @GetMapping("/api/v1/milestones/{id}")
    public ResponseEntity<ApiResponse<MilestoneResponse>> getMilestoneById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        MilestoneResponse milestone = milestoneService.getMilestoneById(id, principal);
        return ResponseEntity.ok(ApiResponse.success(milestone));
    }

    @Operation(summary = "Update an existing milestone")
    @PutMapping("/api/v1/milestones/{id}")
    public ResponseEntity<ApiResponse<MilestoneResponse>> updateMilestone(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMilestoneRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        MilestoneResponse updated = milestoneService.updateMilestone(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success("Milestone updated successfully", updated));
    }

    @Operation(summary = "Delete a milestone")
    @DeleteMapping("/api/v1/milestones/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMilestone(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        milestoneService.deleteMilestone(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Milestone deleted successfully", null));
    }
}
