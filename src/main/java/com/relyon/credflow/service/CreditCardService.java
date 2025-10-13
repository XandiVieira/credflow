package com.relyon.credflow.service;

import com.relyon.credflow.model.category.Category;
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
}
