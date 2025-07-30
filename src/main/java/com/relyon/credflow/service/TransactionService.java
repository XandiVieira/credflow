package com.relyon.credflow.service;

import com.relyon.credflow.exception.CsvProcessingException;
import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.repository.DescriptionMappingRepository;
import com.relyon.credflow.repository.TransactionRepository;

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

import com.relyon.credflow.utils.NormalizationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository repository;
    private final DescriptionMappingRepository mappingRepository;
    private final AccountService accountService;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public List<Transaction> importFromBanrisulCSV(MultipartFile file, Long accountId) {
        log.info("Iniciando importação do arquivo CSV: {}", file.getOriginalFilename());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            var account = accountService.findById(accountId);
            var mappings = preloadMappings(account);
            var newMappings = new HashMap<String, DescriptionMapping>();

            var transactions = reader.lines()
                    .dropWhile(line -> !line.matches("^\\d{2}/\\d{2}/\\d{4}.*"))
                    .map(line -> parseLineToTransaction(line, account, mappings, newMappings))
                    .flatMap(Optional::stream)
                    .filter(this::isNotDuplicate)
                    .map(repository::save)
                    .toList();

            if (!newMappings.isEmpty()) {
                log.info("Salvando {} novos mapeamentos detectados durante a importação", newMappings.size());
                mappingRepository.saveAll(newMappings.values());
            }

            log.info("Importação concluída. Transações salvas: {}", transactions.size());
            return transactions;

        } catch (Exception e) {
            log.error("Erro ao processar CSV: {}", e.getMessage(), e);
            throw new CsvProcessingException("Erro ao processar CSV: " + e.getMessage(), e);
        }
    }

    private boolean isNotDuplicate(Transaction t) {
        var exists = repository.existsByChecksum(t.getChecksum());
        if (exists) log.debug("Transação duplicada ignorada (checksum={}): {}", t.getChecksum(), t);
        return !exists;
    }

    private Optional<Transaction> parseLineToTransaction(
            String line,
            Account account,
            Map<String, DescriptionMapping> existingMappings,
            Map<String, DescriptionMapping> newMappings) {

        try {
            var parts = line.split(";", 4);
            var date = LocalDate.parse(parts[0].trim(), formatter);
            var description = parts[1].replace("\"", "").trim();
            var value = new BigDecimal(parts[2].replace("R$", "").replace(".", "").replace(",", ".").trim());

            var normalized = NormalizationUtils.normalizeDescription(description);

            var mapping = existingMappings.get(normalized);
            if (mapping == null) {
                mapping = newMappings.get(normalized);
            }

            log.info("Descrição original='{}' | Normalizada='{}' | Mapping encontrado={} | Simplified='{}' | Category='{}'",
                    description,
                    normalized,
                    mapping != null,
                    mapping != null ? mapping.getSimplifiedDescription() : "null",
                    mapping != null ? mapping.getCategory() : "null"
            );


            if (mapping == null) {
                log.debug("Adicionando novo mapeamento para descrição ausente: {}", description);
                mapping = DescriptionMapping.builder()
                        .originalDescription(description)
                        .normalizedDescription(normalized)
                        .simplifiedDescription(null)
                        .category(null)
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
            log.warn("Linha ignorada devido a erro de parsing: [{}] - {}", line, ex.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, DescriptionMapping> preloadMappings(Account account) {
        return mappingRepository.findAllByAccount(account).stream()
                .collect(Collectors.toMap(DescriptionMapping::getNormalizedDescription, Function.identity()));
    }

    public Transaction create(Transaction transaction) {
        log.info("Criando nova transação: {}", transaction);
        saveMappingIfNotExists(transaction.getDescription(), transaction.getSimplifiedDescription(), transaction.getCategory());
        return repository.save(transaction);
    }

    public Transaction update(Long id, Transaction updated) {
        log.info("Atualizando transação ID {}: {}", id, updated);

        return repository.findById(id).map(existing -> {
            updateTransactionFields(existing, updated);
            saveMappingIfNotExists(updated.getDescription(), updated.getSimplifiedDescription(), updated.getCategory());
            return repository.save(existing);
        }).orElseThrow(() -> {
            log.warn("Transação com ID {} não encontrada para atualização", id);
            return new ResourceNotFoundException("Transação não encontrada com ID " + id);
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

    public List<Transaction> findFiltered(String responsible, String category, String startDate, String endDate, String sortKey) {
        var effResponsible = responsible != null ? responsible : "Ambos";
        var start = startDate != null ? LocalDate.parse(startDate) : null;
        var end = endDate != null ? LocalDate.parse(endDate) : null;

        var sort = resolveSort(sortKey);
        log.info("Consultando transações: responsible={}, category={}, start={}, end={}, sort={}", effResponsible, category, start, end, sortKey);

        return category == null ?
                repository.findByResponsibleAndDateRange(effResponsible, start, end, sort) :
                repository.findByResponsibleAndCategoryAndDateRange(effResponsible, category, start, end, sort);
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

    public Optional<Transaction> findById(Long id) {
        log.debug("Buscando transação por ID: {}", id);
        return repository.findById(id);
    }

    public void delete(Long id) {
        log.info("Removendo transação ID: {}", id);
        if (!repository.existsById(id)) {
            log.warn("Transação com ID {} não encontrada para exclusão", id);
            throw new ResourceNotFoundException("Transação não encontrada com ID " + id);
        }
        repository.deleteById(id);
    }

    private void saveMappingIfNotExists(String description, String simplified, String category) {
        mappingRepository.findByOriginalDescriptionIgnoreCase(description).ifPresentOrElse(
                _ -> log.debug("Mapeamento já existente para descrição: {}", description),
                () -> {
                    log.debug("Salvando novo mapeamento para descrição: {}", description);
                    var mapping = DescriptionMapping.builder()
                            .originalDescription(description)
                            .simplifiedDescription(simplified)
                            .category(category)
                            .build();
                    mappingRepository.save(mapping);
                });
    }

    public void applyMappingToExistingTransactions(Account account, String originalDescription, String simplified, String category) {
        var normalized = NormalizationUtils.normalizeDescription(originalDescription);
        var affected = mappingRepository.findByAccountAndNormalizedDescription(account, normalized);
        affected.forEach(transaction -> {
            transaction.setSimplifiedDescription(simplified);
            transaction.setCategory(category);
        });
        mappingRepository.saveAll(affected);
    }

}