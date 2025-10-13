package com.relyon.credflow.service;

import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.repository.CreditCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class CreditCardService {

    private final CreditCardRepository creditCardRepository;

    public List<CreditCard> findAll(Long accountId) {
        log.info("Fetching all credit cards for account {}", accountId);
        return creditCardRepository.findAllByAccountId(accountId);
    }

    public CreditCard findById(Long id) {
        log.info("Fetching credit card with id {}", id);
        return creditCardRepository.findById(id).orElse(null);
    }

    public CreditCard create(CreditCard creditCard, Long accountId) {
        log.info("Creating credit card");
        return creditCardRepository.save(creditCard);
    }
}
