package com.relyon.credflow.service;

import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.credit_card.CreditCardResponseDTO;
import com.relyon.credflow.model.mapper.CreditCardMapper;
import com.relyon.credflow.repository.CreditCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class CreditCardService {

    private final CreditCardRepository creditCardRepository;
    private final CreditCardMapper creditCardMapper;

    public List<CreditCardResponseDTO> findAll(Long accountId) {
        log.info("Fetching all credit cards for account {}", accountId);
        List<CreditCard> creditCards = creditCardRepository.findAllByAccountId(accountId);
        return creditCards.stream().map(creditCard -> {
            var creditCardResponse = creditCardMapper.toDTO(creditCard);
            creditCardResponse.setAvailableCreditLimit(computeAvailableLimit(creditCardResponse.getId()));
            return creditCardResponse;
        }).toList();

    }

    public CreditCardResponseDTO findById(Long id) {
        log.info("Fetching credit card with id {}", id);
        CreditCard creditCard = creditCardRepository.findById(id).orElse(null);
        var creditCardResponse = creditCardMapper.toDTO(creditCard);
        creditCardResponse.setAvailableCreditLimit(computeAvailableLimit(id));
        return creditCardResponse;
    }

    public CreditCard create(CreditCard creditCard, Long accountId) {
        log.info("Creating credit card");
        creditCard.setAccount(Account.builder().id(accountId).build());
        return creditCardRepository.save(creditCard);
    }

    public BigDecimal computeAvailableLimit(Long creditCardId) {
        return BigDecimal.ZERO;
    }
}
