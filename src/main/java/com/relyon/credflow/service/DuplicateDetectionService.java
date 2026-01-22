package com.relyon.credflow.service;

import com.relyon.credflow.constant.BusinessConstants;
import com.relyon.credflow.model.transaction.DuplicateGroupDTO;
import com.relyon.credflow.model.transaction.DuplicateGroupDTO.DuplicateTransactionDTO;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.transaction.TransactionSource;
import com.relyon.credflow.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DuplicateDetectionService {

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<DuplicateTransactionDTO> findPotentialDuplicatesForManualEntry(
            Long accountId,
            LocalDate date,
            BigDecimal value
    ) {
        var windowDays = BusinessConstants.Detection.DUPLICATE_SEARCH_WINDOW_DAYS;
        var startDate = date.minusDays(windowDays);
        var endDate = date.plusDays(windowDays);

        log.debug("Searching for potential duplicates: accountId={}, date={}, value={}, window=[{}, {}]",
                accountId, date, value, startDate, endDate);

        return transactionRepository.findPotentialDuplicates(accountId, startDate, endDate, value)
                .stream()
                .filter(transaction -> transaction.getSource() == TransactionSource.CSV_IMPORT)
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DuplicateGroupDTO> findAllPotentialDuplicates(Long accountId) {
        log.info("Finding all potential duplicates for account {}", accountId);

        var allTransactions = transactionRepository.findAllByAccountId(accountId);

        var grouped = groupByDateWindowAndValue(allTransactions);

        return grouped.entrySet().stream()
                .filter(entry -> hasMixedSources(entry.getValue()))
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> DuplicateGroupDTO.builder()
                        .groupKey(entry.getKey())
                        .transactions(entry.getValue().stream().map(this::toDto).toList())
                        .build())
                .toList();
    }

    private LinkedHashMap<String, List<Transaction>> groupByDateWindowAndValue(List<Transaction> transactions) {
        var grouped = new LinkedHashMap<String, List<Transaction>>();
        var windowDays = BusinessConstants.Detection.DUPLICATE_SEARCH_WINDOW_DAYS;

        for (var transaction : transactions) {
            var matched = false;

            for (var entry : grouped.entrySet()) {
                var groupTransaction = entry.getValue().get(0);
                if (isWithinWindow(transaction, groupTransaction, windowDays) && hasSameValue(transaction, groupTransaction)) {
                    entry.getValue().add(transaction);
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                var key = generateGroupKey(transaction);
                var list = new ArrayList<Transaction>();
                list.add(transaction);
                grouped.put(key, list);
            }
        }

        return grouped;
    }

    private boolean isWithinWindow(Transaction first, Transaction second, int windowDays) {
        var daysDiff = Math.abs(first.getDate().toEpochDay() - second.getDate().toEpochDay());
        return daysDiff <= windowDays;
    }

    private boolean hasSameValue(Transaction first, Transaction second) {
        return first.getValue().compareTo(second.getValue()) == 0;
    }

    private boolean hasMixedSources(List<Transaction> transactions) {
        var sources = transactions.stream()
                .map(Transaction::getSource)
                .collect(Collectors.toSet());

        return sources.contains(TransactionSource.CSV_IMPORT) && sources.contains(TransactionSource.MANUAL);
    }

    private String generateGroupKey(Transaction transaction) {
        return transaction.getDate().toString() + "|" + transaction.getValue().toPlainString();
    }

    private DuplicateTransactionDTO toDto(Transaction transaction) {
        return DuplicateTransactionDTO.builder()
                .id(transaction.getId())
                .date(transaction.getDate())
                .description(transaction.getDescription())
                .simplifiedDescription(transaction.getSimplifiedDescription())
                .value(transaction.getValue())
                .source(transaction.getSource())
                .categoryName(transaction.getCategory() != null ? transaction.getCategory().getName() : null)
                .creditCardNickname(transaction.getCreditCard() != null ? transaction.getCreditCard().getNickname() : null)
                .build();
    }
}
