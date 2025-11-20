package com.relyon.credflow.controller;

import com.relyon.credflow.model.transaction.InstallmentGroupRequestDTO;
import com.relyon.credflow.model.transaction.InstallmentGroupResponseDTO;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.service.InstallmentGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/installment-groups")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Installment Groups", description = "Bulk installment management endpoints")
public class InstallmentGroupController {

    private final InstallmentGroupService installmentGroupService;

    @PostMapping
    @Operation(
            summary = "Create installment group",
            description = "Creates multiple installment transactions at once with sequential dates"
    )
    @ApiResponse(responseCode = "201", description = "Installment group created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Category or credit card not found")
    public ResponseEntity<InstallmentGroupResponseDTO> createInstallmentGroup(
            @Valid @RequestBody InstallmentGroupRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("POST /installment-groups for account {}", user.getAccountId());
        var result = installmentGroupService.createInstallmentGroup(request, user.getAccountId());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{installmentGroupId}")
    @Operation(
            summary = "Get installment group",
            description = "Retrieves all installments in a group with summary information"
    )
    @ApiResponse(responseCode = "200", description = "Installment group found")
    @ApiResponse(responseCode = "404", description = "Installment group not found")
    public ResponseEntity<InstallmentGroupResponseDTO> getInstallmentGroup(
            @PathVariable String installmentGroupId,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET /installment-groups/{} for account {}", installmentGroupId, user.getAccountId());
        var result = installmentGroupService.getInstallmentGroup(installmentGroupId, user.getAccountId());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{installmentGroupId}")
    @Operation(
            summary = "Update installment group",
            description = "Updates all installments in the group with new values (description, amount, category, credit card, responsible users)"
    )
    @ApiResponse(responseCode = "200", description = "Installment group updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Installment group, category, or credit card not found")
    public ResponseEntity<InstallmentGroupResponseDTO> updateInstallmentGroup(
            @PathVariable String installmentGroupId,
            @Valid @RequestBody InstallmentGroupRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("PUT /installment-groups/{} for account {}", installmentGroupId, user.getAccountId());
        var result = installmentGroupService.updateInstallmentGroup(
                installmentGroupId, user.getAccountId(), request);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{installmentGroupId}/description")
    @Operation(
            summary = "Update installment group description",
            description = "Updates the description for all installments in the group"
    )
    @ApiResponse(responseCode = "200", description = "Description updated successfully")
    @ApiResponse(responseCode = "404", description = "Installment group not found")
    public ResponseEntity<InstallmentGroupResponseDTO> updateDescription(
            @PathVariable String installmentGroupId,
            @Parameter(description = "New description for all installments")
            @RequestParam String description,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("PATCH /installment-groups/{}/description for account {}",
                installmentGroupId, user.getAccountId());
        var result = installmentGroupService.updateInstallmentGroupDescription(
                installmentGroupId, user.getAccountId(), description);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{installmentGroupId}")
    @Operation(
            summary = "Delete installment group",
            description = "Deletes all installments in the group"
    )
    @ApiResponse(responseCode = "204", description = "Installment group deleted successfully")
    @ApiResponse(responseCode = "404", description = "Installment group not found")
    public ResponseEntity<Void> deleteInstallmentGroup(
            @PathVariable String installmentGroupId,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("DELETE /installment-groups/{} for account {}", installmentGroupId, user.getAccountId());
        installmentGroupService.deleteInstallmentGroup(installmentGroupId, user.getAccountId());
        return ResponseEntity.noContent().build();
    }
}
