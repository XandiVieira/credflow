package com.relyon.credflow.service;

import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.credit_card.CreditCardResponseDTO;
import com.relyon.credflow.model.mapper.CreditCardMapper;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.CreditCardRepository;
import com.relyon.credflow.repository.UserRepository;
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
    private final UserRepository userRepository;

    public List<CreditCardResponseDTO> findAll(Long accountId) {
        log.info("Fetching all credit cards for account {}", accountId);
        List<CreditCard> creditCards = creditCardRepository.findAllByAccountId(accountId);
        return creditCards.stream().map(creditCard -> {
            var creditCardResponse = creditCardMapper.toDTO(creditCard);
            creditCardResponse.setAvailableCreditLimit(computeAvailableLimit(creditCardResponse.getId()));
            return creditCardResponse;
        }).toList();

    }

    public CreditCardResponseDTO findById(Long id, Long accountId) {
        log.info("Fetching credit card with id {} for account {}", id, accountId);
        CreditCard creditCard = creditCardRepository.findByIdAndAccountId(id, accountId).orElse(null);
        var creditCardResponse = creditCardMapper.toDTO(creditCard);
        creditCardResponse.setAvailableCreditLimit(computeAvailableLimit(id));
        return creditCardResponse;
    }

    public CreditCard create(CreditCard creditCard, Long accountId, Long holderId) {
        log.info("Creating credit card for account {} with holder {}", accountId, holderId);

        User holder = userRepository.findByIdAndAccountId(holderId, accountId)
                .orElseThrow(() -> new IllegalArgumentException("Holder not found or does not belong to this account"));

        creditCard.setAccount(Account.builder().id(accountId).build());
        creditCard.setHolder(holder);
        return creditCardRepository.save(creditCard);
    }

    public BigDecimal computeAvailableLimit(Long creditCardId) {
        return BigDecimal.ZERO;
    }
}
