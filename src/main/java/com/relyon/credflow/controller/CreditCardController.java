package com.relyon.credflow.controller;

import com.relyon.credflow.model.credit_card.CreditCardResponseDTO;
import com.relyon.credflow.model.mapper.CreditCardMapper;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.service.CreditCardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/credit-cards")
@RequiredArgsConstructor
@Slf4j
public class CreditCardController {

    private final CreditCardService creditCardService;
    private final CreditCardMapper creditCardMapper;

    @GetMapping
    public ResponseEntity<List<CreditCardResponseDTO>> getAllByAccount(
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET all credit cards for account {}", user.getAccountId());
        var creditCards = creditCardService.findAll(user.getAccountId());
        var response = creditCards.stream().map(creditCardMapper::toDTO).toList();
        return ResponseEntity.ok(response);
    }
}
