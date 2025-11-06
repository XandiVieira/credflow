package com.relyon.credflow.controller;

import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.credit_card.CreditCardRequestDTO;
import com.relyon.credflow.model.credit_card.CreditCardResponseDTO;
import com.relyon.credflow.model.mapper.CreditCardMapper;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.service.CreditCardService;
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
public class CreditCardController {

    private final CreditCardService creditCardService;
    private final CreditCardMapper creditCardMapper;

    @GetMapping
    public ResponseEntity<List<CreditCardResponseDTO>> getAllByAccountId(
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET all credit cards for account {}", user.getAccountId());
        var creditCards = creditCardService.findAll(user.getAccountId());
        return ResponseEntity.ok(creditCards);
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
}
