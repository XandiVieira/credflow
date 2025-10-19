package com.relyon.credflow.controller;

import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.category.CategoryRequestDTO;
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

    @GetMapping
    public ResponseEntity<List<CreditCardResponseDTO>> getAllByAccountId(
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET all credit cards for account {}", user.getAccountId());
        var creditCards = creditCardService.findAll(user.getAccountId());
        var response = creditCards.stream().map(creditCardMapper::toDTO).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreditCardResponseDTO> getCreditCardById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET credit card ID: {}", id);
        var creditCard = creditCardService.findById(id);
        var response = creditCardMapper.toDTO(creditCard);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CreditCardResponseDTO> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreditCardRequestDTO dto) {
        log.info("POST to create credit card for account {}", user.getAccountId());
        CreditCard entity = creditCardMapper.toEntity(dto);
        CreditCard saved = creditCardService.create(entity, user.getAccountId());
        return ResponseEntity.ok(creditCardMapper.toDTO(saved));
    }
}
