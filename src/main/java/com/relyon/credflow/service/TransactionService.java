package com.relyon.credflow.service;

import com.relyon.credflow.exception.CsvProcessingException;
import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.DescriptionMappingRepository;
import com.relyon.credflow.repository.TransactionRepository;
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

    private final DateTimeFormatter banrisulCsvDate = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ---------- CSV import ----------

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
                    null // responsibles will be empty on CSV
            );
            tx.setChecksum(DigestUtils.sha256Hex(line.trim()));
            tx.setAccount(account);
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

    // ---------- CRUD ----------

    @Transactional
    public Transaction create(Transaction tx, Long accountId) {
        tx.setAccount(accountService.findById(accountId));
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

    public Transaction update(Long id, Transaction updated, Long accountId) {
        log.info("Updating transaction ID {}: {}", id, updated);

        return repository.findByIdAndAccountId(id, accountId).map(existing -> {
            existing.setDate(updated.getDate());
            existing.setDescription(updated.getDescription());
            existing.setSimplifiedDescription(updated.getSimplifiedDescription());
            existing.setCategory(updated.getCategory());
            existing.setValue(updated.getValue());
            existing.setResponsibles(resolveResponsiblesForAccount(updated.getResponsibles(), accountId));

            saveMappingIfNotExists(
                    updated.getDescription(), updated.getSimplifiedDescription(),
                    updated.getCategory(), accountService.findById(accountId)
            );
            return repository.save(existing);
        }).orElseThrow(() -> {
            log.warn("Transaction with ID {} not found for update", id);
            return new ResourceNotFoundException("Transaction not found with ID " + id);
        });
    }

    // ---------- Search / filter ----------

    public List<Transaction> findByFilters(
            Long accountId,
            LocalDate fromDate,
            LocalDate toDate,
            String descriptionContains,
            String simplifiedContains,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            List<Long> responsibleUserIds,
            List<Long> categoryIds,
            Sort sort
    ) {
        var descPattern = toLikePattern(descriptionContains);
        var simpPattern = toLikePattern(simplifiedContains);

        var safeSort = (sort == null || sort.isUnsorted())
                ? Sort.by(Sort.Order.desc("date"))
                : sort;

        var respIds = (responsibleUserIds == null || responsibleUserIds.isEmpty()) ? null : responsibleUserIds;
        var catIds  = (categoryIds == null || categoryIds.isEmpty()) ? null : categoryIds;

        log.info("Querying transactions: from={}, to={}, desc~'{}', simp~'{}', min={}, max={}, respIds={}, catIds={}, sort={}",
                fromDate, toDate, descPattern, simpPattern, minAmount, maxAmount, respIds, catIds, safeSort);

        return repository.search(
                accountId,
                descPattern,
                simpPattern,
                fromDate,
                toDate,
                minAmount,
                maxAmount,
                respIds,
                catIds,
                safeSort
        );
    }

    private static String toLikePattern(String s) {
        if (s == null) return null;
        var trimmed = s.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("null")) return null;
        return "%" + trimmed.toLowerCase(Locale.ROOT) + "%";
    }

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

    // ---------- Mapping helper ----------

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
}