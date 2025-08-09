package com.relyon.credflow.service;

import com.relyon.credflow.exception.CsvProcessingException;
import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.repository.DescriptionMappingRepository;
import com.relyon.credflow.repository.TransactionRepository;
import com.relyon.credflow.utils.NormalizationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository repository;
    private final DescriptionMappingRepository mappingRepository;
    private final AccountService accountService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public List<Transaction> importFromBanrisulCSV(MultipartFile file, Long accountId) {
        var account = accountService.findById(accountId);
        log.info("Starting CSV import: {}", file.getOriginalFilename());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            var mappings = preloadMappings(accountId);
            var newMappings = new HashMap<String, DescriptionMapping>();

            var transactions = reader.lines()
                    .dropWhile(line -> !line.matches("^\\d{2}/\\d{2}/\\d{4}.*"))
                    .map(line -> parseLineToTransaction(line, account, mappings, newMappings))
                    .flatMap(Optional::stream)
                    .filter(this::isNotDuplicate)
                    .map(repository::save)
                    .toList();

            if (!newMappings.isEmpty()) {
                log.info("Saving {} new mappings detected during import", newMappings.size());
                mappingRepository.saveAll(newMappings.values());
            }

            log.info("Import completed. Transactions saved: {}", transactions.size());
            return transactions;

        } catch (Exception e) {
            log.error("Error processing CSV: {}", e.getMessage(), e);
            throw new CsvProcessingException("CSV processing error: " + e.getMessage(), e);
        }
    }

    private boolean isNotDuplicate(Transaction t) {
        boolean exists = repository.existsByChecksum(t.getChecksum());
        if (exists) log.info("Duplicate transaction skipped (checksum={}): {}", t.getChecksum(), t);
        return !exists;
    }

    private Optional<Transaction> parseLineToTransaction(String line, Account account, Map<String, DescriptionMapping> existingMappings, Map<String, DescriptionMapping> newMappings) {
        try {
            var parts = line.split(";", 4);
            var date = LocalDate.parse(parts[0].trim(), formatter);
            var description = parts[1].replace("\"", "").trim();
            var value = new BigDecimal(parts[2].replace("R$", "").replace(".", "").replace(",", ".").trim());

            var normalized = NormalizationUtils.normalizeDescription(description);
            var mapping = Optional.ofNullable(existingMappings.get(normalized)).orElse(newMappings.get(normalized));

            if (mapping == null) {
                mapping = DescriptionMapping.builder()
                        .originalDescription(description)
                        .normalizedDescription(normalized)
                        .account(account)
                        .build();
                newMappings.put(normalized, mapping);
            }

            var transaction = new Transaction(
                    date,
                    description,
                    mapping.getSimplifiedDescription(),
                    mapping.getCategory() != null ? mapping.getCategory() : "Não Identificado",
                    value,
                    "Ambos"
            );
            transaction.setChecksum(DigestUtils.sha256Hex(line.trim()));
            transaction.setAccount(account);
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

    public Transaction create(Transaction transaction) {
        log.info("Creating new transaction: {}", transaction);
        saveMappingIfNotExists(transaction.getDescription(), transaction.getSimplifiedDescription(), transaction.getCategory(), transaction.getAccount());
        return repository.save(transaction);
    }

    public Transaction update(Long id, Transaction updated, Long accountId) {
        log.info("Updating transaction ID {}: {}", id, updated);

        return repository.findByIdAndAccountId(id, accountId).map(existing -> {
            updateTransactionFields(existing, updated);
            saveMappingIfNotExists(updated.getDescription(), updated.getSimplifiedDescription(), updated.getCategory(), accountService.findById(accountId));
            return repository.save(existing);
        }).orElseThrow(() -> {
            log.warn("Transaction with ID {} not found for update", id);
            return new ResourceNotFoundException("Transaction not found with ID " + id);
        });
    }

    private void updateTransactionFields(Transaction existing, Transaction updated) {
        existing.setDate(updated.getDate());
        existing.setDescription(updated.getDescription());
        existing.setSimplifiedDescription(updated.getSimplifiedDescription());
        existing.setCategory(updated.getCategory());
        existing.setValue(updated.getValue());
        existing.setResponsible(updated.getResponsible());
    }

    public List<Transaction> findFiltered(Long accountId, String responsible, String category, String startDate, String endDate, String sortKey) {
        var effResponsible = responsible != null ? responsible : "Ambos";
        var start = startDate != null ? LocalDate.parse(startDate) : null;
        var end = endDate != null ? LocalDate.parse(endDate) : null;

        var sort = resolveSort(sortKey);
        log.info("Querying transactions: responsible={}, category={}, start={}, end={}, sort={}", effResponsible, category, start, end, sortKey);

        return category == null ?
                repository.findByAccountIdAndResponsibleAndDateBetween(accountId, effResponsible, start, end, sort) :
                repository.findByAccountIdAndResponsibleAndCategoryAndDateBetween(accountId, effResponsible, category, start, end, sort);
    }

    private Sort resolveSort(String sortKey) {
        return switch (sortKey) {
            case "date" -> Sort.by("date").ascending();
            case "value" -> Sort.by("value").ascending();
            case "valueDesc" -> Sort.by("value").descending();
            case "description" -> Sort.by("description").ascending();
            case "descriptionDesc" -> Sort.by("description").descending();
            default -> Sort.by("date").descending();
        };
    }

    public Optional<Transaction> findById(Long id, Long accountId) {
        log.info("Finding transaction by ID: {}", id);
        return repository.findByIdAndAccountId(id, accountId);
    }

    public void delete(Long id, Long accountId) {
        log.info("Deleting transaction ID: {}", id);
        var transaction = repository.findByIdAndAccountId(id, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID " + id));
        repository.delete(transaction);
    }

    private void saveMappingIfNotExists(String description, String simplified, String category, Account account) {
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

    public void applyMappingToExistingTransactions(Long accountId, String originalDescription, String simplified, String category) {
        var affected = repository.findByAccountIdAndDescriptionIgnoreCase(accountId, originalDescription);
        affected.forEach(t -> {
            t.setSimplifiedDescription(simplified);
            t.setCategory(category);
        });
        repository.saveAll(affected);
    }
}