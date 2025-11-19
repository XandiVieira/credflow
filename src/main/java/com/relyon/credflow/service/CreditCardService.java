package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.credit_card.CreditCardResponseDTO;
import com.relyon.credflow.model.credit_card.CreditCardSelectDTO;
import com.relyon.credflow.model.mapper.CreditCardMapper;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.CreditCardRepository;
import com.relyon.credflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class CreditCardService {

    private final CreditCardRepository creditCardRepository;
    private final CreditCardMapper creditCardMapper;
    private final UserRepository userRepository;
    private final CreditCardBillingService billingService;
    private final LocalizedMessageTranslationService translationService;

    public Page<CreditCardResponseDTO> findAll(Long accountId, int page, int size) {
        log.info("Fetching credit cards for account {} (page={}, size={})", accountId, page, size);
        var pageable = PageRequest.of(page, size);
        var creditCards = creditCardRepository.findAllByAccountId(accountId, pageable);
        return creditCards.map(this::mapToDTO);
    }

    public CreditCardResponseDTO findById(Long id, Long accountId) {
        log.info("Fetching credit card with id {} for account {}", id, accountId);
        CreditCard creditCard = creditCardRepository.findByIdAndAccountId(id, accountId)
                .orElseThrow(() -> {
                    log.warn("Credit card with id {} not found for account {}", id, accountId);
                    return new ResourceNotFoundException("resource.creditCard.notFound", id);
                });
        return mapToDTO(creditCard);
    }

    public CreditCard create(CreditCard creditCard, Long accountId, Long holderId) {
        log.info("Creating credit card for account {} with holder {}", accountId, holderId);

        User holder = userRepository.findByIdAndAccountId(holderId, accountId)
                .orElseThrow(() -> new IllegalArgumentException(translationService.translateMessage("creditCard.holderNotFound")));

        creditCard.setAccount(Account.builder().id(accountId).build());
        creditCard.setHolder(holder);
        return creditCardRepository.save(creditCard);
    }

    public CreditCard update(Long id, Long accountId, CreditCard updated, Long holderId) {
        log.info("Updating credit card ID: {} for account {}", id, accountId);

        CreditCard existing = creditCardRepository.findByIdAndAccountId(id, accountId)
                .orElseThrow(() -> {
                    log.warn("Credit card with id {} not found for account {}", id, accountId);
                    return new ResourceNotFoundException("resource.creditCard.notFound", id);
                });

        User holder = userRepository.findByIdAndAccountId(holderId, accountId)
                .orElseThrow(() -> new IllegalArgumentException(translationService.translateMessage("creditCard.holderNotFound")));

        existing.setNickname(updated.getNickname());
        existing.setBrand(updated.getBrand());
        existing.setTier(updated.getTier());
        existing.setIssuer(updated.getIssuer());
        existing.setLastFourDigits(updated.getLastFourDigits());
        existing.setClosingDay(updated.getClosingDay());
        existing.setDueDay(updated.getDueDay());
        existing.setCreditLimit(updated.getCreditLimit());
        existing.setHolder(holder);

        var saved = creditCardRepository.save(existing);
        log.info("Credit card ID: {} successfully updated", id);
        return saved;
    }

    public void delete(Long id, Long accountId) {
        log.info("Deleting credit card ID: {} for account {}", id, accountId);

        if (!creditCardRepository.existsByIdAndAccountId(id, accountId)) {
            log.warn("Credit card with id {} not found for account {}", id, accountId);
            throw new ResourceNotFoundException("resource.creditCard.notFound", id);
        }

        creditCardRepository.deleteById(id);
        log.info("Credit card ID: {} successfully deleted", id);
    }

    private CreditCardResponseDTO mapToDTO(CreditCard entity) {
        CreditCardResponseDTO dto = creditCardMapper.toDTO(entity);
        dto.setAvailableCreditLimit(billingService.computeAvailableLimit(dto.getId()));
        dto.setCurrentBill(billingService.computeCurrentBill(dto.getId(), entity.getClosingDay(), entity.getDueDay()));
        return dto;
    }

    /**
     * Returns a simple list of credit cards with only id and description (nickname + last digits)
     * for dropdowns/selects
     */
    public List<CreditCardSelectDTO> findAllSelectByAccount(Long accountId) {
        log.info("Fetching select credit card list for account {}", accountId);
        return creditCardRepository.findAllByAccountId(accountId).stream()
                .map(card -> {
                    String description = card.getNickname() + " - " + card.getLastFourDigits();
                    return new CreditCardSelectDTO(card.getId(), description);
                })
                .toList();
    }
}
