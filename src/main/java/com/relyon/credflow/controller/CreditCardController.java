package com.relyon.credflow.controller;

import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.credit_card.CreditCardRequestDTO;
import com.relyon.credflow.model.credit_card.CreditCardResponseDTO;
import com.relyon.credflow.model.credit_card.CreditCardSelectDTO;
import com.relyon.credflow.model.mapper.CreditCardMapper;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.service.CreditCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/credit-cards")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Credit Cards", description = "Credit card management endpoints")
public class CreditCardController {

    private final CreditCardService creditCardService;
    private final CreditCardMapper creditCardMapper;

    @GetMapping
    public ResponseEntity<org.springframework.data.domain.Page<CreditCardResponseDTO>> getAllByAccountId(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("GET all credit cards for account {} (page={}, size={})", user.getAccountId(), page, size);
        var creditCards = creditCardService.findAll(user.getAccountId(), page, size);
        return ResponseEntity.ok(creditCards);
    }

    @GetMapping("/select")
    public ResponseEntity<List<CreditCardSelectDTO>> getAllSelect(
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET select credit card list for account {}", user.getAccountId());
        var response = creditCardService.findAllSelectByAccount(user.getAccountId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreditCardResponseDTO> getCreditCardById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET credit card ID: {} for account {}", id, user.getAccountId());
        var creditCard = creditCardService.findById(id, user.getAccountId());
        return ResponseEntity.ok(creditCard);
    }

    @PostMapping
    public ResponseEntity<CreditCardResponseDTO> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreditCardRequestDTO dto) {
        log.info("POST to create credit card for account {} with holder {}", user.getAccountId(), dto.getHolderId());
        CreditCard entity = creditCardMapper.toEntity(dto);
        CreditCard saved = creditCardService.create(entity, user.getAccountId(), dto.getHolderId());
        return ResponseEntity.ok(creditCardMapper.toDTO(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update credit card", description = "Updates an existing credit card for the authenticated account")
    @ApiResponse(responseCode = "200", description = "Credit card successfully updated")
    @ApiResponse(responseCode = "404", description = "Credit card not found or does not belong to account")
    @ApiResponse(responseCode = "400", description = "Invalid holder ID or validation error")
    public ResponseEntity<CreditCardResponseDTO> update(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreditCardRequestDTO dto) {
        log.info("PUT to update credit card ID: {} for account {}", id, user.getAccountId());
        CreditCard patch = creditCardMapper.toEntity(dto);
        CreditCard updated = creditCardService.update(id, user.getAccountId(), patch, dto.getHolderId());
        return ResponseEntity.ok(creditCardMapper.toDTO(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete credit card", description = "Soft deletes a credit card for the authenticated account")
    @ApiResponse(responseCode = "204", description = "Credit card successfully deleted")
    @ApiResponse(responseCode = "404", description = "Credit card not found or does not belong to account")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("DELETE credit card ID: {} for account {}", id, user.getAccountId());
        creditCardService.delete(id, user.getAccountId());
        log.info("Credit card ID: {} successfully deleted", id);
        return ResponseEntity.noContent().build();
    }
}
