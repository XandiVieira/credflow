package com.relyon.credflow.controller;

import com.relyon.credflow.model.mapper.UserMapper;
import com.relyon.credflow.model.user.AuthRequest;
import com.relyon.credflow.model.user.UserRequestDTO;
import com.relyon.credflow.model.user.UserResponseDTO;
import com.relyon.credflow.service.AuthenticationService;
import com.relyon.credflow.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/auth")
@Tag(name = "Authentication", description = "Authentication and password management endpoints")
public class AuthenticationController {

    private final AuthenticationService authService;
    private final UserMapper userMapper;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRequestDTO request) {
        log.info("Registering user: {}", request.getEmail());
        var saved = authService.register(request);
        return ResponseEntity
                .created(URI.create("/users/" + saved.getId()))
                .body(userMapper.toDto(saved));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody AuthRequest authRequest) {
        log.info("Logging in user: {}", authRequest.getEmail());
        return ResponseEntity.ok(authService.login(authRequest));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Initiate password reset", description = "Sends password reset email to user")
    @ApiResponse(responseCode = "200", description = "Password reset email sent successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Password reset requested for: {}", request.email());
        passwordResetService.initiatePasswordReset(request.email());
        return ResponseEntity.ok(Map.of("message", "If an account exists with this email, a password reset link has been sent"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Resets user password using valid reset token")
    @ApiResponse(responseCode = "200", description = "Password reset successfully")
    @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Resetting password with token");
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    @GetMapping("/validate-reset-token")
    @Operation(summary = "Validate reset token", description = "Validates if a password reset token is still valid")
    @ApiResponse(responseCode = "200", description = "Token is valid")
    @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    public ResponseEntity<Map<String, String>> validateResetToken(@RequestParam String token) {
        log.info("Validating reset token");
        passwordResetService.validateResetToken(token);
        return ResponseEntity.ok(Map.of("message", "Token is valid"));
    }

    record ForgotPasswordRequest(@NotBlank @Email String email) {
    }

    record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank String newPassword
    ) {
    }
}