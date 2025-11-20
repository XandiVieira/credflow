package com.relyon.credflow.controller;

import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.model.user.UserPreferencesDTO;
import com.relyon.credflow.service.UserPreferencesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/users/preferences")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Preferences", description = "User preferences and settings management")
public class UserPreferencesController {

    private final UserPreferencesService userPreferencesService;

    @GetMapping
    @Operation(
            summary = "Get user preferences",
            description = "Retrieves preferences for the authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "Preferences retrieved successfully")
    public ResponseEntity<UserPreferencesDTO> getPreferences(
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET /users/preferences for user {} in account {}", user.getUserId(), user.getAccountId());
        var preferences = userPreferencesService.getPreferences(user.getUserId(), user.getAccountId());
        return ResponseEntity.ok(preferences);
    }

    @PutMapping
    @Operation(
            summary = "Update user preferences",
            description = "Updates preferences for the authenticated user"
    )
    @ApiResponse(responseCode = "200", description = "Preferences updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid preferences data")
    public ResponseEntity<UserPreferencesDTO> updatePreferences(
            @Valid @RequestBody UserPreferencesDTO dto,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("PUT /users/preferences for user {} in account {}", user.getUserId(), user.getAccountId());
        var updated = userPreferencesService.updatePreferences(user.getUserId(), user.getAccountId(), dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping
    @Operation(
            summary = "Delete user preferences",
            description = "Deletes preferences and resets to defaults for the authenticated user"
    )
    @ApiResponse(responseCode = "204", description = "Preferences deleted successfully")
    public ResponseEntity<Void> deletePreferences(
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("DELETE /users/preferences for user {} in account {}", user.getUserId(), user.getAccountId());
        userPreferencesService.deletePreferences(user.getUserId(), user.getAccountId());
        return ResponseEntity.noContent().build();
    }
}
