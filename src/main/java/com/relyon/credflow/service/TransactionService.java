package com.relyon.credflow.service;

import com.relyon.credflow.exception.CsvProcessingException;
import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.transaction.TransactionFilter;
import com.relyon.credflow.model.transaction.TransactionSource;
import com.relyon.credflow.model.transaction.TransactionType;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.CreditCardRepository;
import com.relyon.credflow.repository.DescriptionMappingRepository;
import com.relyon.credflow.repository.TransactionRepository;
import com.relyon.credflow.specification.Sorts;
import com.relyon.credflow.specification.TransactionFilterNormalizer;
import com.relyon.credflow.specification.TransactionSpecFactory;
import com.relyon.credflow.utils.NormalizationUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository repository;
    private final DescriptionMappingRepository mappingRepository;
    private final AccountService accountService;
    private final UserService userService;
    private final CategoryService categoryService;
    private final CreditCardRepository creditCardRepository;
    private final RefundDetectionService refundDetectionService;
    private final LocalizedMessageTranslationService translationService;

    private final DateTimeFormatter banrisulCsvDate = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public List<Transaction> importFromBanrisulCSV(MultipartFile file, Long accountId) {
        var account = accountService.findById(accountId);
        log.info("Starting CSV import: {}", file.getOriginalFilename());

        try (var reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            var existing = preloadMappings(accountId);
            var pending = new HashMap<String, DescriptionMapping>();

            var saved = reader.lines()
                    .dropWhile(line -> !line.matches("^\\d{2}/\\d{2}/\\d{4}.*"))
                    .map(line -> parseLine(line, account, existing, pending))
                    .flatMap(Optional::stream)
                    .filter(this::isNotDuplicateByChecksum)
                    .map(repository::save)
                    .toList();

            if (!pending.isEmpty()) {
                log.info("Saving {} new mappings detected during import", pending.size());
                mappingRepository.saveAll(pending.values());
            }

            log.info("Running refund detection on {} imported transactions", saved.size());
            saved.forEach(refundDetectionService::detectAndLinkReversal);

            log.info("Import completed. Transactions saved: {}", saved.size());
            return saved;

        } catch (Exception e) {
            log.error("Error processing CSV: {}", e.getMessage(), e);
            throw new CsvProcessingException("csv.processing.error", e, e.getMessage());
        }
    }

    private boolean isNotDuplicateByChecksum(Transaction t) {
        if (repository.existsByChecksum(t.getChecksum())) {
            log.info("Duplicate transaction skipped (raw checksum): {}", t.getDescription());
            return false;
        }
        if (t.getNormalizedChecksum() != null && repository.existsByNormalizedChecksum(t.getNormalizedChecksum())) {
            log.info("Duplicate transaction skipped (normalized checksum): {}", t.getDescription());
            return false;
        }
        return true;
    }

    private Optional<Transaction> parseLine(
            String line,
            Account account,
            Map<String, DescriptionMapping> existing,
            Map<String, DescriptionMapping> pending
    ) {
        try {
            var parts = line.split(";", 4);
            var date = LocalDate.parse(parts[0].trim(), banrisulCsvDate);
            var description = parts[1].replace("\"", "").trim();
            var value = new BigDecimal(parts[2].replace("R$", "").replace(".", "").replace(",", ".").trim());

            var normalized = NormalizationUtils.normalizeDescription(description);
            var mapping = Optional.ofNullable(existing.get(normalized)).orElse(pending.get(normalized));

            if (mapping == null) {
                mapping = DescriptionMapping.builder()
                        .originalDescription(description)
                        .normalizedDescription(normalized)
                        .account(account)
                        .build();
                pending.put(normalized, mapping);
            }

            var transaction = new Transaction(
                    date,
                    description,
                    mapping.getSimplifiedDescription(),
                    mapping.getCategory(),
                    value,
                    null
            );
            var checksum = DigestUtils.sha256Hex(line.trim());
            var normalizedChecksum = NormalizationUtils.generateNormalizedChecksum(date, description, value, account.getId());

            transaction.setChecksum(checksum);
            transaction.setNormalizedChecksum(normalizedChecksum);
            transaction.setAccount(account);
            transaction.setTransactionType(TransactionType.ONE_TIME);

            initializeSourceTrackingFields(transaction, TransactionSource.CSV_IMPORT, null);
            transaction.setOriginalChecksum(checksum);

            return Optional.of(transaction);

        } catch (Exception ex) {
            log.warn("Line ignored due to parsing error: [{}] - {}", line, ex.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, DescriptionMapping> preloadMappings(Long accountId) {
        return mappingRepository.findAllByAccountId(accountId).stream()
                .collect(Collectors.toMap(DescriptionMapping::getNormalizedDescription, Function.identity()));
    }

    @Transactional
    public Transaction create(Transaction tx, Long accountId) {
        validateTransactionTypeAndInstallments(tx);

        tx.setAccount(accountService.findById(accountId));

        if (tx.getCategory() != null && tx.getCategory().getId() != null) {
            tx.setCategory(categoryService.findById(tx.getCategory().getId(), accountId));
        }

        if (tx.getCreditCard() != null && tx.getCreditCard().getId() != null) {
            var creditCard = creditCardRepository.findByIdAndAccountId(tx.getCreditCard().getId(), accountId)
                    .orElseThrow(() -> new IllegalArgumentException(translationService.translateMessage("resource.creditCard.notFound")));
            tx.setCreditCard(creditCard);
        }

        tx.setResponsibleUsers(resolveResponsibleUsersForAccount(tx.getResponsibleUsers(), accountId));

        initializeSourceTrackingFields(tx, TransactionSource.MANUAL, null);

        saveMappingIfNotExists(tx.getDescription(), tx.getSimplifiedDescription(), tx.getCategory(), tx.getAccount());

        var saved = repository.save(tx);

        refundDetectionService.detectAndLinkReversal(saved);

        return saved;
    }

    private Set<User> resolveResponsibleUsersForAccount(Set<User> userStubs, Long accountId) {
        if (userStubs == null || userStubs.isEmpty()) return Collections.emptySet();

        var ids = userStubs.stream().map(User::getId).toList();
        var found = userService.findAllByIds(ids);
        if (found.size() != ids.size()) {
            throw new IllegalArgumentException(translationService.translateMessage("users.notFound", ids));
        }

        for (var user : found) {
            if (user.getAccount() == null || !user.getAccount().getId().equals(accountId)) {
                throw new IllegalArgumentException(translationService.translateMessage("user.accountMismatch", user.getId()));
            }
        }
        return new LinkedHashSet<>(found);
    }

    @Transactional
    public Transaction update(Long id, Transaction updated, Long accountId) {
        log.info("Updating transaction ID {}: {}", id, updated);
        validateTransactionTypeAndInstallments(updated);

        return repository.findByIdAndAccountId(id, accountId).map(existing -> {
            markAsEditedIfImported(existing);

            existing.setDate(updated.getDate());
            existing.setDescription(updated.getDescription());
            existing.setSimplifiedDescription(updated.getSimplifiedDescription());
            existing.setTransactionType(updated.getTransactionType());
            existing.setCurrentInstallment(updated.getCurrentInstallment());
            existing.setTotalInstallments(updated.getTotalInstallments());
            existing.setInstallmentGroupId(updated.getInstallmentGroupId());

            if (updated.getCategory() != null && updated.getCategory().getId() != null) {
                existing.setCategory(categoryService.findById(updated.getCategory().getId(), accountId));
            } else {
                existing.setCategory(null);
            }

            if (updated.getCreditCard() != null && updated.getCreditCard().getId() != null) {
                var creditCard = creditCardRepository.findByIdAndAccountId(updated.getCreditCard().getId(), accountId)
                        .orElseThrow(() -> new IllegalArgumentException(translationService.translateMessage("resource.creditCard.notFound")));
                existing.setCreditCard(creditCard);
            } else {
                existing.setCreditCard(null);
            }

            existing.setValue(updated.getValue());
            existing.setResponsibleUsers(resolveResponsibleUsersForAccount(updated.getResponsibleUsers(), accountId));

            saveMappingIfNotExists(
                    updated.getDescription(),
                    updated.getSimplifiedDescription(),
                    existing.getCategory(),
                    accountService.findById(accountId)
            );

            var saved = repository.save(existing);

            refundDetectionService.detectAndLinkReversal(saved);

            return saved;
        }).orElseThrow(() -> new ResourceNotFoundException("resource.transaction.notFound", id));
    }

    @Transactional(readOnly = true)
    public List<Transaction> search(TransactionFilter filter, Sort sort) {
        var normalized = TransactionFilterNormalizer.normalize(filter);
        var spec = TransactionSpecFactory.from(normalized);
        var safeSort = Sorts.resolve(sort);
        return repository.findAll(spec, safeSort);
    }

    @Transactional(readOnly = true)
    public Optional<Transaction> findById(Long id, Long accountId) {
        log.info("Finding transaction by ID: {}", id);
        return repository.findByIdAndAccountId(id, accountId);
    }

    public void delete(Long id, Long accountId) {
        log.info("Deleting transaction ID: {}", id);
        var transaction = repository.findByIdAndAccountId(id, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("resource.transaction.notFound", id));
        repository.delete(transaction);
    }

    @Transactional
    public void bulkDelete(List<Long> transactionIds, Long accountId) {
        log.info("Bulk deleting {} transactions for account {}", transactionIds.size(), accountId);

        var transactions = transactionIds.stream()
                .map(id -> repository.findByIdAndAccountId(id, accountId)
                        .orElseThrow(() -> new ResourceNotFoundException("resource.transaction.notFound", id)))
                .toList();

        repository.deleteAll(transactions);
        log.info("Successfully deleted {} transactions", transactions.size());
    }

    @Transactional
    public List<Transaction> bulkUpdateCategory(List<Long> transactionIds, Long categoryId, Long accountId) {
        log.info("Bulk updating category to {} for {} transactions in account {}", categoryId, transactionIds.size(), accountId);

        var category = categoryId != null ? categoryService.findById(categoryId, accountId) : null;

        var transactions = transactionIds.stream()
                .map(id -> repository.findByIdAndAccountId(id, accountId)
                        .orElseThrow(() -> new ResourceNotFoundException("resource.transaction.notFound", id)))
                .peek(transaction -> {
                    markAsEditedIfImported(transaction);
                    transaction.setCategory(category);
                })
                .toList();

        var updated = repository.saveAll(transactions);
        log.info("Successfully updated category for {} transactions", updated.size());
        return updated;
    }

    @Transactional
    public List<Transaction> bulkUpdateResponsibleUsers(List<Long> transactionIds, List<Long> responsibleUserIds, Long accountId) {
        log.info("Bulk updating responsible users to {} for {} transactions in account {}", responsibleUserIds, transactionIds.size(), accountId);

        var responsibleUsers = resolveResponsibleUsersForAccount(
                responsibleUserIds.stream().map(id -> {
                    var user = new User();
                    user.setId(id);
                    return user;
                }).collect(Collectors.toSet()),
                accountId
        );

        var transactions = transactionIds.stream()
                .map(id -> repository.findByIdAndAccountId(id, accountId)
                        .orElseThrow(() -> new ResourceNotFoundException("resource.transaction.notFound", id)))
                .peek(transaction -> {
                    markAsEditedIfImported(transaction);
                    transaction.setResponsibleUsers(responsibleUsers);
                })
                .toList();

        var updated = repository.saveAll(transactions);
        log.info("Successfully updated responsible users for {} transactions", updated.size());
        return updated;
    }

    private void saveMappingIfNotExists(String description, String simplified, Category category, Account account) {
        var normalized = NormalizationUtils.normalizeDescription(description);
        mappingRepository.findByNormalizedDescriptionAndAccountId(normalized, account.getId()).ifPresentOrElse(
                m -> log.debug("Mapping already exists for normalized: {}", normalized),
                () -> {
                    log.info("Saving new mapping for normalized: {}", normalized);
                    var mapping = DescriptionMapping.builder()
                            .originalDescription(description)
                            .normalizedDescription(normalized)
                            .simplifiedDescription(simplified)
                            .category(category)
                            .account(account)
                            .build();
                    mappingRepository.save(mapping);
                });
    }

    public void applyMappingToExistingTransactions(Long accountId, String originalDescription, String simplified, Category category) {
        var affected = repository.findByAccountIdAndDescriptionIgnoreCase(accountId, originalDescription);
        affected.forEach(transaction -> {
            transaction.setSimplifiedDescription(simplified);
            transaction.setCategory(category);
        });
        repository.saveAll(affected);
    }

    private void validateTransactionTypeAndInstallments(Transaction transaction) {
        if (transaction.getTransactionType() == null) {
            throw new IllegalArgumentException(translationService.translateMessage("transaction.type.required"));
        }

        if (transaction.getTransactionType() == TransactionType.INSTALLMENT) {
            if (transaction.getCurrentInstallment() == null) {
                throw new IllegalArgumentException(translationService.translateMessage("transaction.installment.current.required"));
            }
            if (transaction.getTotalInstallments() == null) {
                throw new IllegalArgumentException(translationService.translateMessage("transaction.installment.total.required"));
            }
            if (transaction.getCurrentInstallment() <= 0) {
                throw new IllegalArgumentException(translationService.translateMessage("transaction.installment.current.invalid"));
            }
            if (transaction.getTotalInstallments() <= 0) {
                throw new IllegalArgumentException(translationService.translateMessage("transaction.installment.total.invalid"));
            }
            if (transaction.getCurrentInstallment() > transaction.getTotalInstallments()) {
                throw new IllegalArgumentException(translationService.translateMessage("transaction.installment.current.exceeds"));
            }
        } else {
            if (transaction.getCurrentInstallment() != null || transaction.getTotalInstallments() != null) {
                log.warn("Clearing installment fields for non-installment transaction type: {}", transaction.getTransactionType());
                transaction.setCurrentInstallment(null);
                transaction.setTotalInstallments(null);
                transaction.setInstallmentGroupId(null);
            }
        }
    }

    private void initializeSourceTrackingFields(Transaction transaction, TransactionSource source, String importBatchId) {
        transaction.setSource(source);
        transaction.setImportBatchId(importBatchId);
        transaction.setWasEditedAfterImport(false);
        transaction.setIsReversal(false);

        if (transaction.getChecksum() != null && transaction.getOriginalChecksum() == null) {
            transaction.setOriginalChecksum(transaction.getChecksum());
        }

        log.debug("Initialized source tracking: source={}, batchId={}", source, importBatchId);
    }

    private void markAsEditedIfImported(Transaction transaction) {
        if (transaction.getSource() != TransactionSource.MANUAL && !Boolean.TRUE.equals(transaction.getWasEditedAfterImport())) {
            log.info("Marking transaction {} as edited after import", transaction.getId());
            transaction.setWasEditedAfterImport(true);
        }
    }
}
