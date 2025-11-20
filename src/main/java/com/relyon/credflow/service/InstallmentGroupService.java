package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.transaction.*;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.CategoryRepository;
import com.relyon.credflow.repository.CreditCardRepository;
import com.relyon.credflow.repository.TransactionRepository;
import com.relyon.credflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstallmentGroupService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CreditCardRepository creditCardRepository;
    private final UserRepository userRepository;
    private final LocalizedMessageTranslationService translationService;

    @Transactional
    public InstallmentGroupResponseDTO createInstallmentGroup(InstallmentGroupRequestDTO request, Long accountId) {
        log.info("Creating installment group with {} installments for account {}",
                request.getTotalInstallments(), accountId);

        var category = categoryRepository.findByIdAndAccountId(request.getCategoryId(), accountId)
                .orElseThrow(() -> new ResourceNotFoundException("resource.category.notFound", request.getCategoryId()));

        CreditCard creditCard = null;
        if (request.getCreditCardId() != null) {
            creditCard = creditCardRepository.findByIdAndAccountId(request.getCreditCardId(), accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("resource.creditCard.notFound", request.getCreditCardId()));
        }

        Set<User> responsibleUsers = new HashSet<>();
        if (request.getResponsibleUserIds() != null && !request.getResponsibleUserIds().isEmpty()) {
            responsibleUsers = new HashSet<>(userRepository.findAllByIdInAndAccountId(
                    request.getResponsibleUserIds(), accountId));

            if (responsibleUsers.size() != request.getResponsibleUserIds().size()) {
                throw new IllegalArgumentException(
                        translationService.translateMessage("users.notFound",
                                request.getResponsibleUserIds().toString()));
            }
        }

        var installmentGroupId = UUID.randomUUID().toString();
        var installmentAmount = request.getTotalAmount()
                .divide(BigDecimal.valueOf(request.getTotalInstallments()), 2, RoundingMode.HALF_UP);

        var finalCreditCard = creditCard;
        var finalResponsibleUsers = responsibleUsers;

        for (int i = 1; i <= request.getTotalInstallments(); i++) {
            var installmentDate = request.getFirstInstallmentDate().plusMonths(i - 1);

            var transaction = Transaction.builder()
                    .description(request.getDescription())
                    .value(installmentAmount)
                    .transactionType(TransactionType.INSTALLMENT)
                    .date(installmentDate)
                    .category(category)
                    .creditCard(finalCreditCard)
                    .currentInstallment(i)
                    .totalInstallments(request.getTotalInstallments())
                    .installmentGroupId(installmentGroupId)
                    .responsibleUsers(new HashSet<>(finalResponsibleUsers))
                    .source(TransactionSource.MANUAL)
                    .wasEditedAfterImport(false)
                    .isReversal(false)
                    .account(category.getAccount())
                    .build();

            transactionRepository.save(transaction);
        }

        log.info("Created installment group {} with {} installments",
                installmentGroupId, request.getTotalInstallments());

        return getInstallmentGroup(installmentGroupId, accountId);
    }

    @Transactional(readOnly = true)
    public InstallmentGroupResponseDTO getInstallmentGroup(String installmentGroupId, Long accountId) {
        log.info("Fetching installment group {} for account {}", installmentGroupId, accountId);

        var installments = transactionRepository.findByInstallmentGroupIdAndAccountId(installmentGroupId, accountId);

        if (installments.isEmpty()) {
            throw new ResourceNotFoundException("installment.group.notFound", installmentGroupId);
        }

        var paidCount = 0;
        var totalPaid = BigDecimal.ZERO;
        var totalAmount = BigDecimal.ZERO;

        for (var installment : installments) {
            totalAmount = totalAmount.add(installment.getValue());
            if (Boolean.TRUE.equals(installment.getWasEditedAfterImport())) {
                paidCount++;
                totalPaid = totalPaid.add(installment.getValue());
            }
        }

        var firstInstallment = installments.getFirst();

        return InstallmentGroupResponseDTO.builder()
                .installmentGroupId(installmentGroupId)
                .description(firstInstallment.getDescription())
                .totalAmount(totalAmount)
                .totalInstallments(firstInstallment.getTotalInstallments())
                .paidInstallments(paidCount)
                .pendingInstallments(firstInstallment.getTotalInstallments() - paidCount)
                .totalPaid(totalPaid)
                .totalPending(totalAmount.subtract(totalPaid))
                .installments(installments.stream().map(this::mapToResponseDTO).toList())
                .build();
    }

    @Transactional
    public void deleteInstallmentGroup(String installmentGroupId, Long accountId) {
        log.info("Deleting installment group {} for account {}", installmentGroupId, accountId);

        var installments = transactionRepository.findByInstallmentGroupIdAndAccountId(installmentGroupId, accountId);

        if (installments.isEmpty()) {
            throw new ResourceNotFoundException("installment.group.notFound", installmentGroupId);
        }

        installments.forEach(transaction -> transactionRepository.deleteById(transaction.getId()));

        log.info("Deleted {} installments from group {}", installments.size(), installmentGroupId);
    }

    @Transactional
    public InstallmentGroupResponseDTO updateInstallmentGroup(
            String installmentGroupId, Long accountId, InstallmentGroupRequestDTO request) {

        log.info("Updating installment group {} in account {}", installmentGroupId, accountId);

        var installments = transactionRepository.findByInstallmentGroupIdAndAccountId(installmentGroupId, accountId);

        if (installments.isEmpty()) {
            throw new ResourceNotFoundException("installment.group.notFound", installmentGroupId);
        }

        var category = categoryRepository.findByIdAndAccountId(request.getCategoryId(), accountId)
                .orElseThrow(() -> new ResourceNotFoundException("resource.category.notFound", request.getCategoryId()));

        CreditCard creditCard = null;
        if (request.getCreditCardId() != null) {
            creditCard = creditCardRepository.findByIdAndAccountId(request.getCreditCardId(), accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("resource.creditCard.notFound", request.getCreditCardId()));
        }

        Set<User> responsibleUsers = new HashSet<>();
        if (request.getResponsibleUserIds() != null && !request.getResponsibleUserIds().isEmpty()) {
            responsibleUsers = new HashSet<>(userRepository.findAllByIdInAndAccountId(
                    request.getResponsibleUserIds(), accountId));

            if (responsibleUsers.size() != request.getResponsibleUserIds().size()) {
                throw new IllegalArgumentException(
                        translationService.translateMessage("users.notFound",
                                request.getResponsibleUserIds().toString()));
            }
        }

        var installmentAmount = request.getTotalAmount()
                .divide(BigDecimal.valueOf(request.getTotalInstallments()), 2, RoundingMode.HALF_UP);

        var finalCreditCard = creditCard;
        var finalResponsibleUsers = responsibleUsers;

        for (var transaction : installments) {
            transaction.setDescription(request.getDescription());
            transaction.setValue(installmentAmount);
            transaction.setCategory(category);
            transaction.setCreditCard(finalCreditCard);
            transaction.setResponsibleUsers(new HashSet<>(finalResponsibleUsers));
            transactionRepository.save(transaction);
        }

        log.info("Updated {} installments in group {}", installments.size(), installmentGroupId);

        return getInstallmentGroup(installmentGroupId, accountId);
    }

    @Transactional
    public InstallmentGroupResponseDTO updateInstallmentGroupDescription(
            String installmentGroupId, Long accountId, String newDescription) {

        log.info("Updating description for installment group {} in account {}", installmentGroupId, accountId);

        var installments = transactionRepository.findByInstallmentGroupIdAndAccountId(installmentGroupId, accountId);

        if (installments.isEmpty()) {
            throw new ResourceNotFoundException("installment.group.notFound", installmentGroupId);
        }

        installments.forEach(transaction -> {
            transaction.setDescription(newDescription);
            transactionRepository.save(transaction);
        });

        log.info("Updated description for {} installments in group {}", installments.size(), installmentGroupId);

        return getInstallmentGroup(installmentGroupId, accountId);
    }

    private TransactionResponseDTO mapToResponseDTO(Transaction transaction) {
        var dto = new TransactionResponseDTO();
        dto.setId(transaction.getId());
        dto.setDescription(transaction.getDescription());
        dto.setValue(transaction.getValue());
        dto.setDate(transaction.getDate());
        dto.setTransactionType(transaction.getTransactionType());
        dto.setCurrentInstallment(transaction.getCurrentInstallment());
        dto.setTotalInstallments(transaction.getTotalInstallments());
        dto.setInstallmentGroupId(transaction.getInstallmentGroupId());
        dto.setSource(transaction.getSource());
        dto.setWasEditedAfterImport(transaction.getWasEditedAfterImport());
        dto.setIsReversal(transaction.getIsReversal());

        if (transaction.getCategory() != null) {
            dto.setCategory(transaction.getCategory().getName());
        }

        if (transaction.getCreditCard() != null) {
            var creditCardDto = new TransactionResponseDTO.CreditCardDTO();
            creditCardDto.setId(transaction.getCreditCard().getId());
            creditCardDto.setNickname(transaction.getCreditCard().getNickname());
            creditCardDto.setBrand(transaction.getCreditCard().getBrand());
            creditCardDto.setLastFourDigits(transaction.getCreditCard().getLastFourDigits());
            dto.setCreditCard(creditCardDto);
        }

        return dto;
    }
}
