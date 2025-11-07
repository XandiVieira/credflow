package com.relyon.credflow.service;

import com.relyon.credflow.exception.CsvProcessingException;
import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.transaction.TransactionFilter;
import com.relyon.credflow.model.transaction.TransactionType;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.CreditCardRepository;
import com.relyon.credflow.repository.DescriptionMappingRepository;
import com.relyon.credflow.repository.TransactionRepository;
import com.relyon.credflow.specification.Sorts;
import com.relyon.credflow.specification.TransactionFilterNormalizer;
import com.relyon.credflow.specification.TransactionSpecFactory;
import com.relyon.credflow.utils.NormalizationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

            log.info("Import completed. Transactions saved: {}", saved.size());
            return saved;

        } catch (Exception e) {
            log.error("Error processing CSV: {}", e.getMessage(), e);
            throw new CsvProcessingException("CSV processing error: " + e.getMessage(), e);
        }
    }

    private boolean isNotDuplicateByChecksum(Transaction t) {
        var exists = repository.existsByChecksum(t.getChecksum());
        if (exists) log.info("Duplicate transaction skipped (checksum={}): {}", t.getChecksum(), t);
        return !exists;
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

            var tx = new Transaction(
                    date,
                    description,
                    mapping.getSimplifiedDescription(),
                    mapping.getCategory() != null ? mapping.getCategory() : new Category("Não Identificado", account),
                    value,
                    null
            );
            tx.setChecksum(DigestUtils.sha256Hex(line.trim()));
            tx.setAccount(account);
            tx.setTransactionType(TransactionType.EVENTUAL); // Default for CSV imports
            return Optional.of(tx);

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
            CreditCard creditCard = creditCardRepository.findByIdAndAccountId(tx.getCreditCard().getId(), accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Credit card not found or does not belong to this account"));
            tx.setCreditCard(creditCard);
        }

        tx.setResponsibles(resolveResponsiblesForAccount(tx.getResponsibles(), accountId));
        saveMappingIfNotExists(tx.getDescription(), tx.getSimplifiedDescription(), tx.getCategory(), tx.getAccount());
        return repository.save(tx);
    }

    private Set<User> resolveResponsiblesForAccount(Set<User> stubs, Long accountId) {
        if (stubs == null || stubs.isEmpty()) return Collections.emptySet();

        var ids = stubs.stream().map(User::getId).toList();
        var found = userService.findAllByIds(ids);
        if (found.size() != ids.size()) {
            throw new IllegalArgumentException("One or more users not found: " + ids);
        }

        for (User u : found) {
            if (u.getAccount() == null || !u.getAccount().getId().equals(accountId)) {
                throw new IllegalArgumentException("User " + u.getId() + " does not belong to this account.");
            }
        }
        return new LinkedHashSet<>(found);
    }

    @Transactional
    public Transaction update(Long id, Transaction updated, Long accountId) {
        log.info("Updating transaction ID {}: {}", id, updated);
        validateTransactionTypeAndInstallments(updated);

        return repository.findByIdAndAccountId(id, accountId).map(existing -> {
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
                CreditCard creditCard = creditCardRepository.findByIdAndAccountId(updated.getCreditCard().getId(), accountId)
                        .orElseThrow(() -> new IllegalArgumentException("Credit card not found or does not belong to this account"));
                existing.setCreditCard(creditCard);
            } else {
                existing.setCreditCard(null);
            }

            existing.setValue(updated.getValue());
            existing.setResponsibles(resolveResponsiblesForAccount(updated.getResponsibles(), accountId));

            saveMappingIfNotExists(
                    updated.getDescription(),
                    updated.getSimplifiedDescription(),
                    existing.getCategory(),
                    accountService.findById(accountId)
            );
            return repository.save(existing);
        }).orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID " + id));
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
        var tx = repository.findByIdAndAccountId(id, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID " + id));
        repository.delete(tx);
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
        affected.forEach(t -> {
            t.setSimplifiedDescription(simplified);
            t.setCategory(category);
        });
        repository.saveAll(affected);
    }

    /**
     * Validates transaction type and installment fields consistency
     */
    private void validateTransactionTypeAndInstallments(Transaction tx) {
        if (tx.getTransactionType() == null) {
            throw new IllegalArgumentException("Transaction type is required");
        }

        if (tx.getTransactionType() == TransactionType.PARCELA) {
            // When type is PARCELA, installment fields are required
            if (tx.getCurrentInstallment() == null) {
                throw new IllegalArgumentException("Current installment is required for installment transactions");
            }
            if (tx.getTotalInstallments() == null) {
                throw new IllegalArgumentException("Total installments is required for installment transactions");
            }
            if (tx.getCurrentInstallment() <= 0) {
                throw new IllegalArgumentException("Current installment must be greater than zero");
            }
            if (tx.getTotalInstallments() <= 0) {
                throw new IllegalArgumentException("Total installments must be greater than zero");
            }
            if (tx.getCurrentInstallment() > tx.getTotalInstallments()) {
                throw new IllegalArgumentException("Current installment cannot be greater than total installments");
            }
        } else {
            // For EVENTUAL and RECORRENTE, installment fields should be null
            if (tx.getCurrentInstallment() != null || tx.getTotalInstallments() != null) {
                log.warn("Clearing installment fields for non-installment transaction type: {}", tx.getTransactionType());
                tx.setCurrentInstallment(null);
                tx.setTotalInstallments(null);
                tx.setInstallmentGroupId(null);
            }
        }
    }
}