package com.relyon.credflow.controller;

import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.account.AccountRequestDTO;
import com.relyon.credflow.model.account.AccountResponseDTO;
import com.relyon.credflow.model.mapper.AccountMapper;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.service.AccountService;
import com.relyon.credflow.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Accounts", description = "Account management and invitation endpoints")
public class AccountController {

    private final AccountService accountService;
    private final AccountMapper accountMapper;
    private final EmailService emailService;

    @GetMapping
    public ResponseEntity<List<AccountResponseDTO>> findAll(
            @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("GET to fetch accounts for user {}", user.getUserId());
        var result = accountService.findAllByUserId(user.getUserId()).stream()
                .map(accountMapper::toDto)
                .toList();
        log.info("Found {} accounts for user {}", result.size(), user.getUserId());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponseDTO> findById(@PathVariable Long id) {
        log.info("GET to fetch account by ID {}", id);
        return accountService.findByIdOptional(id)
                .map(a -> {
                    log.info("Account with ID {} found", id);
                    return ResponseEntity.ok(accountMapper.toDto(a));
                })
                .orElseGet(() -> {
                    log.warn("Account with ID {} not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody AccountRequestDTO requestDTO) {

        log.info("PUT to update account with ID {}", id);
        Account patch = accountMapper.toEntity(requestDTO);
        var updated = accountService.update(id, patch);
        log.info("Account with ID {} successfully updated", id);
        return ResponseEntity.ok(accountMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE account with ID {}", id);
        accountService.delete(id);
        log.info("Account with ID {} successfully deleted", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/invite-code")
    @Operation(summary = "Get account invite code", description = "Returns the invite code for the account")
    @ApiResponse(responseCode = "200", description = "Invite code retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Account not found")
    public ResponseEntity<Map<String, String>> getInviteCode(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("GET invite code for account {}", id);
        var account = accountService.findById(id);
        return ResponseEntity.ok(Map.of("inviteCode", account.getInviteCode()));
    }

    @PostMapping("/{id}/regenerate-invite-code")
    @Operation(summary = "Regenerate account invite code", description = "Generates a new invite code for the account, invalidating the old one")
    @ApiResponse(responseCode = "200", description = "Invite code regenerated successfully")
    @ApiResponse(responseCode = "404", description = "Account not found")
    public ResponseEntity<Map<String, String>> regenerateInviteCode(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("Regenerating invite code for account {}", id);
        var newCode = accountService.regenerateInviteCode(id);
        return ResponseEntity.ok(Map.of("inviteCode", newCode));
    }

    @PostMapping("/join")
    @Operation(summary = "Join account with invite code", description = "Adds the current user to an account using an invite code")
    @ApiResponse(responseCode = "200", description = "Successfully joined account")
    @ApiResponse(responseCode = "404", description = "Invalid invite code")
    @ApiResponse(responseCode = "400", description = "User already in account")
    public ResponseEntity<AccountResponseDTO> joinAccount(
            @Valid @RequestBody JoinAccountRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("User {} joining account with code {}", user.getUserId(), request.inviteCode());
        var account = accountService.findByInviteCode(request.inviteCode());
        return ResponseEntity.ok(accountMapper.toDto(account));
    }

    @PostMapping("/{id}/send-invitation")
    @Operation(summary = "Send invitation email", description = "Sends an invitation email to a user to join the account")
    @ApiResponse(responseCode = "200", description = "Invitation email sent successfully")
    @ApiResponse(responseCode = "404", description = "Account not found")
    public ResponseEntity<Map<String, String>> sendInvitation(
            @PathVariable Long id,
            @Valid @RequestBody SendInvitationRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("Sending invitation for account {} to email {}", id, request.email());
        var account = accountService.findById(id);
        emailService.sendAccountInvitationEmail(request.email(), account.getInviteCode(), user.getName());
        return ResponseEntity.ok(Map.of("message", "Invitation sent successfully"));
    }

    public record JoinAccountRequest(@NotBlank String inviteCode) {}

    public record SendInvitationRequest(@NotBlank @Email String email) {}
}