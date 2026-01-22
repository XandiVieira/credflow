package com.relyon.credflow.service;

import com.relyon.credflow.constant.BusinessConstants;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundDetectionService {

    private static final int REVERSAL_SEARCH_WINDOW_DAYS = BusinessConstants.Detection.REVERSAL_SEARCH_WINDOW_DAYS;
    private static final double DESCRIPTION_SIMILARITY_THRESHOLD = BusinessConstants.Detection.DESCRIPTION_SIMILARITY_THRESHOLD;
    private final Object reversalDetectionLock = new Object();

    private final TransactionRepository transactionRepository;

    @Transactional
    public Optional<Transaction> detectAndLinkReversal(Transaction transaction) {
        if (transaction.getIsReversal() != null && transaction.getIsReversal()) {
            log.debug("Transaction {} is already marked as reversal, skipping detection", transaction.getId());
            return Optional.empty();
        }

        if (transaction.getValue() == null || transaction.getValue().compareTo(BigDecimal.ZERO) == 0) {
            log.debug("Transaction {} has zero or null value, skipping reversal detection", transaction.getId());
            return Optional.empty();
        }

        if (transaction.getValue().compareTo(BigDecimal.ZERO) > 0) {
            log.debug("Transaction {} is positive (refund/credit), skipping reversal detection", transaction.getId());
            return Optional.empty();
        }

        synchronized (reversalDetectionLock) {
            if (transaction.getIsReversal() != null && transaction.getIsReversal()) {
                log.debug("Transaction {} was marked as reversal while waiting for lock", transaction.getId());
                return Optional.empty();
            }

            var startDate = transaction.getDate().minusDays(REVERSAL_SEARCH_WINDOW_DAYS);
            var endDate = transaction.getDate().plusDays(REVERSAL_SEARCH_WINDOW_DAYS);
            var creditCardId = transaction.getCreditCard() != null ? transaction.getCreditCard().getId() : null;

            log.debug("Searching for potential reversals for transaction {} within {} to {}",
                    transaction.getId(), startDate, endDate);

            var potentialReversals = transactionRepository.findPotentialReversals(
                    transaction.getAccount().getId(),
                    transaction.getId(),
                    transaction.getValue(),
                    startDate,
                    endDate,
                    creditCardId
            );

            return potentialReversals.stream()
                    .filter(candidate -> isLikelyReversal(transaction, candidate))
                    .filter(candidate -> !candidate.getIsReversal())
                    .findFirst()
                    .map(reversal -> linkTransactionsAsReversals(transaction, reversal));
        }
    }

    private boolean isLikelyReversal(Transaction transaction, Transaction candidate) {
        var descriptionSimilarity = calculateDescriptionSimilarity(
                transaction.getDescription(),
                candidate.getDescription()
        );

        log.debug("Comparing transaction {} with candidate {}: similarity = {}, threshold = {}",
                transaction.getId(), candidate.getId(), descriptionSimilarity, DESCRIPTION_SIMILARITY_THRESHOLD);

        return descriptionSimilarity >= DESCRIPTION_SIMILARITY_THRESHOLD;
    }

    private Transaction linkTransactionsAsReversals(Transaction transaction, Transaction reversal) {
        log.info("Linking transactions {} and {} as reversals", transaction.getId(), reversal.getId());

        transaction.setIsReversal(true);
        transaction.setRelatedTransaction(reversal);

        reversal.setIsReversal(true);
        reversal.setRelatedTransaction(transaction);

        transactionRepository.save(transaction);
        transactionRepository.save(reversal);

        return reversal;
    }

    private double calculateDescriptionSimilarity(String desc1, String desc2) {
        if (desc1 == null || desc2 == null) {
            return 0.0;
        }

        var normalized1 = desc1.toLowerCase().trim();
        var normalized2 = desc2.toLowerCase().trim();

        if (normalized1.equals(normalized2)) {
            return 1.0;
        }

        return calculateLevenshteinSimilarity(normalized1, normalized2);
    }

    private double calculateLevenshteinSimilarity(String s1, String s2) {
        var distance = levenshteinDistance(s1, s2);
        var maxLength = Math.max(s1.length(), s2.length());

        if (maxLength == 0) {
            return 1.0;
        }

        return 1.0 - ((double) distance / maxLength);
    }

    private int levenshteinDistance(String s1, String s2) {
        var len1 = s1.length();
        var len2 = s2.length();

        var dp = new int[len1 + 1][len2 + 1];

        for (var i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }

        for (var j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (var i = 1; i <= len1; i++) {
            for (var j = 1; j <= len2; j++) {
                var cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[len1][len2];
    }
}
