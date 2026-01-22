package com.relyon.credflow.service;

import com.relyon.credflow.exception.PdfProcessingException;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import com.relyon.credflow.model.pdf.ParsedCardSection;
import com.relyon.credflow.model.pdf.ParsedCreditCardTransaction;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.transaction.TransactionSource;
import com.relyon.credflow.model.transaction.TransactionType;
import com.relyon.credflow.repository.CreditCardRepository;
import com.relyon.credflow.repository.DescriptionMappingRepository;
import com.relyon.credflow.repository.TransactionRepository;
import com.relyon.credflow.utils.NormalizationUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class BanrisulPdfParserService {

    private static final Pattern CARD_HEADER_PATTERN = Pattern.compile("^(\\d{4})\\s*-\\s*(.+)$");
    private static final Pattern TRANSACTION_PATTERN = Pattern.compile(
            "^(\\d{2}/\\d{2}/\\d{4})\\s+(.+?)\\s+(-?[\\d.,]+)\\s+([\\d.,]+)$"
    );
    private static final Pattern INSTALLMENT_PATTERN = Pattern.compile("(\\d{2})/(\\d{2})");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final TransactionRepository transactionRepository;
    private final CreditCardRepository creditCardRepository;
    private final DescriptionMappingRepository mappingRepository;
    private final AccountService accountService;
    private final RefundDetectionService refundDetectionService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Transaction> importFromPdf(MultipartFile file, Long accountId) {
        validatePdfFile(file);
        log.info("Starting PDF import: {}", file.getOriginalFilename());

        var account = accountService.findById(accountId);
        var pdfText = extractTextFromPdf(file);
        var cardSections = parseCardSections(pdfText);

        log.info("Found {} card sections in PDF", cardSections.size());

        var existingMappings = preloadMappings(accountId);
        var pendingMappings = new HashMap<String, DescriptionMapping>();
        var importedTransactions = new ArrayList<Transaction>();

        for (var section : cardSections) {
            var creditCard = resolveCreditCard(section.lastFourDigits(), section.holderName(), accountId);

            for (var parsed : section.transactions()) {
                buildTransaction(parsed, account, creditCard, existingMappings, pendingMappings)
                        .filter(this::isNotDuplicateByChecksum)
                        .ifPresent(transaction -> {
                            var saved = transactionRepository.save(transaction);
                            importedTransactions.add(saved);
                        });
            }
        }

        if (!pendingMappings.isEmpty()) {
            log.info("Saving {} new mappings detected during PDF import", pendingMappings.size());
            mappingRepository.saveAll(pendingMappings.values());
        }

        log.info("Running refund detection on {} imported transactions", importedTransactions.size());
        importedTransactions.forEach(refundDetectionService::detectAndLinkReversal);

        log.info("PDF import completed. Transactions saved: {}", importedTransactions.size());
        return importedTransactions;
    }

    private void validatePdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new PdfProcessingException("pdf.file.empty");
        }

        var contentType = file.getContentType();
        var filename = file.getOriginalFilename();

        if (contentType != null && !PDF_CONTENT_TYPE.equals(contentType)) {
            throw new PdfProcessingException("pdf.file.invalidType", contentType);
        }

        if (filename != null && !filename.toLowerCase().endsWith(".pdf")) {
            throw new PdfProcessingException("pdf.file.invalidExtension", filename);
        }
    }

    String extractTextFromPdf(MultipartFile file) {
        try (var document = Loader.loadPDF(file.getBytes())) {
            var stripper = new PDFTextStripper();
            var text = stripper.getText(document);
            log.info("Extracted PDF text (first 2000 chars):\n{}", text.substring(0, Math.min(2000, text.length())));
            return text;
        } catch (Exception e) {
            log.error("Failed to extract text from PDF: {}", e.getMessage(), e);
            throw new PdfProcessingException("pdf.extraction.error", e, e.getMessage());
        }
    }

    List<ParsedCardSection> parseCardSections(String pdfText) {
        var sections = new ArrayList<ParsedCardSection>();
        var lines = pdfText.split("\\R");

        log.info("PDF text has {} lines", lines.length);

        String currentCardDigits = null;
        String currentHolderName = null;
        var currentTransactions = new ArrayList<ParsedCreditCardTransaction>();

        for (var line : lines) {
            var trimmedLine = normalizeWhitespace(line);
            if (trimmedLine.isEmpty()) continue;

            var cardMatcher = CARD_HEADER_PATTERN.matcher(trimmedLine);
            if (cardMatcher.matches()) {
                log.info("Found card header: {}", trimmedLine);
                if (currentCardDigits != null && !currentTransactions.isEmpty()) {
                    sections.add(ParsedCardSection.builder()
                            .lastFourDigits(currentCardDigits)
                            .holderName(currentHolderName)
                            .transactions(new ArrayList<>(currentTransactions))
                            .build());
                }
                currentCardDigits = cardMatcher.group(1);
                currentHolderName = cardMatcher.group(2).trim();
                currentTransactions.clear();
                continue;
            }

            if (currentCardDigits != null) {
                parseTransactionLine(trimmedLine, currentCardDigits, currentHolderName)
                        .ifPresent(parsedTransaction -> {
                            log.info("Parsed transaction: {} {} {}", parsedTransaction.date(), parsedTransaction.description(), parsedTransaction.valueBrl());
                            currentTransactions.add(parsedTransaction);
                        });
            }
        }

        if (currentCardDigits != null && !currentTransactions.isEmpty()) {
            sections.add(ParsedCardSection.builder()
                    .lastFourDigits(currentCardDigits)
                    .holderName(currentHolderName)
                    .transactions(new ArrayList<>(currentTransactions))
                    .build());
        }

        log.info("Parsed {} card sections with {} total transactions", sections.size(),
                sections.stream().mapToInt(section -> section.transactions().size()).sum());

        return sections;
    }

    Optional<ParsedCreditCardTransaction> parseTransactionLine(String line, String cardDigits, String holderName) {
        var normalizedLine = normalizeWhitespace(line);
        var matcher = TRANSACTION_PATTERN.matcher(normalizedLine);
        if (!matcher.matches()) {
            if (normalizedLine.matches("^\\d{2}/\\d{2}/\\d{4}.*")) {
                log.debug("Transaction-like line didn't match pattern: [{}]", normalizedLine);
            }
            return Optional.empty();
        }

        try {
            var dateStr = matcher.group(1);
            var description = matcher.group(2).trim();
            var valueBrlStr = matcher.group(3);
            var valueUsdStr = matcher.group(4);

            var date = LocalDate.parse(dateStr, DATE_FORMATTER);
            var valueBrl = parseBrazilianDecimal(valueBrlStr);
            var valueUsd = parseBrazilianDecimal(valueUsdStr);

            Integer currentInstallment = null;
            Integer totalInstallments = null;

            var installmentMatcher = INSTALLMENT_PATTERN.matcher(description);
            if (installmentMatcher.find()) {
                currentInstallment = Integer.parseInt(installmentMatcher.group(1));
                totalInstallments = Integer.parseInt(installmentMatcher.group(2));
            }

            return Optional.of(ParsedCreditCardTransaction.builder()
                    .date(date)
                    .description(description)
                    .valueBrl(valueBrl)
                    .valueUsd(valueUsd)
                    .currentInstallment(currentInstallment)
                    .totalInstallments(totalInstallments)
                    .cardLastFourDigits(cardDigits)
                    .cardHolderName(holderName)
                    .rawLine(normalizedLine)
                    .build());
        } catch (Exception e) {
            log.debug("Failed to parse transaction line: [{}] - {}", normalizedLine, e.getMessage());
            return Optional.empty();
        }
    }

    BigDecimal parseBrazilianDecimal(String value) {
        var normalized = value.replace(".", "").replace(",", ".");
        return new BigDecimal(normalized);
    }

    String normalizeWhitespace(String line) {
        return line
                .replace('\u00A0', ' ')
                .replace('\u2007', ' ')
                .replace('\u202F', ' ')
                .trim();
    }

    private CreditCard resolveCreditCard(String lastFourDigits, String holderName, Long accountId) {
        var candidates = creditCardRepository.findByLastFourDigitsAndAccountId(lastFourDigits, accountId);

        if (candidates.isEmpty()) {
            log.warn("No credit card found with last four digits {} for account {}", lastFourDigits, accountId);
            return null;
        }

        if (candidates.size() == 1) {
            return candidates.getFirst();
        }

        return candidates.stream()
                .filter(card -> fuzzyNameMatch(card.getHolder().getName(), holderName))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("Multiple cards match digits {} but none match holder name {}. Using first.", lastFourDigits, holderName);
                    return candidates.getFirst();
                });
    }

    private boolean fuzzyNameMatch(String cardHolderName, String pdfHolderName) {
        if (cardHolderName == null || pdfHolderName == null) return false;

        var normalizedCard = cardHolderName.toUpperCase().replaceAll("\\s+", " ").trim();
        var normalizedPdf = pdfHolderName.toUpperCase().replaceAll("\\s+", " ").trim();

        if (normalizedCard.equals(normalizedPdf)) return true;

        var cardParts = normalizedCard.split("\\s+");
        var pdfParts = normalizedPdf.split("\\s+");

        if (cardParts.length > 0 && pdfParts.length > 0) {
            var firstNameMatch = cardParts[0].equals(pdfParts[0]);
            var lastNameMatch = cardParts.length > 1 && pdfParts.length > 1 &&
                    cardParts[cardParts.length - 1].equals(pdfParts[pdfParts.length - 1]);
            return firstNameMatch && lastNameMatch;
        }

        return false;
    }

    private Optional<Transaction> buildTransaction(
            ParsedCreditCardTransaction parsed,
            Account account,
            CreditCard creditCard,
            Map<String, DescriptionMapping> existingMappings,
            Map<String, DescriptionMapping> pendingMappings
    ) {
        try {
            var description = parsed.description();
            var normalized = NormalizationUtils.normalizeDescription(description);
            var mapping = Optional.ofNullable(existingMappings.get(normalized))
                    .orElse(pendingMappings.get(normalized));

            if (mapping == null) {
                mapping = DescriptionMapping.builder()
                        .originalDescription(description)
                        .normalizedDescription(normalized)
                        .account(account)
                        .build();
                pendingMappings.put(normalized, mapping);
            }

            var value = computeTransactionValue(parsed.valueBrl());
            var checksum = DigestUtils.sha256Hex(parsed.rawLine().trim());
            var normalizedChecksum = NormalizationUtils.generateNormalizedChecksum(
                    parsed.date(), description, parsed.valueBrl(), account.getId());

            var transactionType = determineTransactionType(parsed);

            var tx = Transaction.builder()
                    .date(parsed.date())
                    .description(description)
                    .simplifiedDescription(mapping.getSimplifiedDescription())
                    .category(mapping.getCategory())
                    .value(value)
                    .account(account)
                    .creditCard(creditCard)
                    .transactionType(transactionType)
                    .currentInstallment(parsed.currentInstallment())
                    .totalInstallments(parsed.totalInstallments())
                    .checksum(checksum)
                    .originalChecksum(checksum)
                    .normalizedChecksum(normalizedChecksum)
                    .source(TransactionSource.CSV_IMPORT)
                    .wasEditedAfterImport(false)
                    .isReversal(false)
                    .build();

            return Optional.of(tx);
        } catch (Exception e) {
            log.warn("Failed to build transaction from parsed data: {} - {}", parsed, e.getMessage());
            return Optional.empty();
        }
    }

    private BigDecimal computeTransactionValue(BigDecimal valueBrl) {
        if (valueBrl.compareTo(BigDecimal.ZERO) < 0) {
            return valueBrl;
        }
        return valueBrl.negate();
    }

    private TransactionType determineTransactionType(ParsedCreditCardTransaction parsed) {
        if (parsed.currentInstallment() != null && parsed.totalInstallments() != null) {
            return TransactionType.INSTALLMENT;
        }
        return TransactionType.ONE_TIME;
    }

    private boolean isNotDuplicateByChecksum(Transaction tx) {
        if (transactionRepository.existsByChecksum(tx.getChecksum())) {
            log.info("Duplicate transaction skipped (raw checksum): {}", tx.getDescription());
            return false;
        }
        if (tx.getNormalizedChecksum() != null &&
                transactionRepository.existsByNormalizedChecksum(tx.getNormalizedChecksum())) {
            log.info("Duplicate transaction skipped (normalized checksum): {}", tx.getDescription());
            return false;
        }
        return true;
    }

    private Map<String, DescriptionMapping> preloadMappings(Long accountId) {
        return mappingRepository.findAllByAccountId(accountId).stream()
                .collect(Collectors.toMap(DescriptionMapping::getNormalizedDescription, Function.identity()));
    }
}
