package com.relyon.credflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.pdf.ParsedCardSection;
import com.relyon.credflow.repository.CreditCardRepository;
import com.relyon.credflow.repository.DescriptionMappingRepository;
import com.relyon.credflow.repository.TransactionRepository;
import com.relyon.credflow.utils.NormalizationUtils;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Banrisul File Parser Integration Tests")
class BanrisulFileParserIntegrationTest {

    private static final String TEST_DATA_PATH = "testdata/banrisul/";
    private static final String PDF_FILE = "fatura_outubro_2025.pdf";
    private static final String CSV_FILE = "fatura_outubro_2025.csv";

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private CreditCardRepository creditCardRepository;
    @Mock
    private DescriptionMappingRepository mappingRepository;
    @Mock
    private AccountService accountService;
    @Mock
    private RefundDetectionService refundDetectionService;

    private BanrisulPdfParserService pdfParserService;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        pdfParserService = new BanrisulPdfParserService(
                transactionRepository,
                creditCardRepository,
                mappingRepository,
                accountService,
                refundDetectionService
        );
        testAccount = Account.builder().id(1L).build();
    }

    @Nested
    @DisplayName("PDF Parsing with Real File")
    class PdfParsingTests {

        private String pdfText;

        @BeforeEach
        void loadPdfText() throws IOException {
            var resourceStream = getClass().getClassLoader().getResourceAsStream(TEST_DATA_PATH + PDF_FILE);
            assumeTrue(resourceStream != null, "PDF test file not found in resources");

            try (var document = Loader.loadPDF(resourceStream.readAllBytes())) {
                var stripper = new PDFTextStripper();
                pdfText = stripper.getText(document);
            }
        }

        @Test
        @DisplayName("Should extract text from PDF with NBSP characters")
        void shouldExtractTextWithNbspCharacters() {
            assertThat(pdfText).isNotEmpty();
            assertThat(pdfText).contains("7152");
            assertThat(pdfText).contains("ALEXANDRE");

            var containsNbsp = pdfText.chars().anyMatch(c -> c == 0x00A0);
            assertThat(containsNbsp).as("PDF should contain NBSP characters").isTrue();
        }

        @Test
        @DisplayName("Should parse multiple card sections")
        void shouldParseMultipleCardSections() {
            var sections = pdfParserService.parseCardSections(pdfText);

            assertThat(sections).isNotEmpty();
            assertThat(sections.size()).isGreaterThanOrEqualTo(2);

            var cardDigits = sections.stream()
                    .map(ParsedCardSection::lastFourDigits)
                    .distinct()
                    .toList();
            assertThat(cardDigits).contains("7152");
        }

        @Test
        @DisplayName("Should parse transactions with installments")
        void shouldParseTransactionsWithInstallments() {
            var sections = pdfParserService.parseCardSections(pdfText);

            var installmentTransactions = sections.stream()
                    .flatMap(s -> s.transactions().stream())
                    .filter(t -> t.currentInstallment() != null && t.totalInstallments() != null)
                    .toList();

            assertThat(installmentTransactions).isNotEmpty();

            var feFloresTransaction = installmentTransactions.stream()
                    .filter(t -> t.description().contains("FeFloresCostura"))
                    .findFirst();

            assertThat(feFloresTransaction).isPresent();
            assertThat(feFloresTransaction.get().currentInstallment()).isEqualTo(2);
            assertThat(feFloresTransaction.get().totalInstallments()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should parse payment transactions with negative values")
        void shouldParsePaymentWithNegativeValues() {
            var sections = pdfParserService.parseCardSections(pdfText);

            var paymentTransaction = sections.stream()
                    .flatMap(s -> s.transactions().stream())
                    .filter(t -> t.description().contains("PGTO HOME/OFFICE BANKING"))
                    .findFirst();

            assertThat(paymentTransaction).isPresent();
            assertThat(paymentTransaction.get().valueBrl()).isNegative();
            assertThat(paymentTransaction.get().valueBrl()).isEqualByComparingTo(new BigDecimal("-12855.13"));
        }

        @Test
        @DisplayName("Should parse foreign transactions with USD values")
        void shouldParseForeignTransactionsWithUsd() {
            var sections = pdfParserService.parseCardSections(pdfText);

            var foreignTransaction = sections.stream()
                    .flatMap(s -> s.transactions().stream())
                    .filter(t -> t.valueUsd() != null && t.valueUsd().compareTo(BigDecimal.ZERO) > 0)
                    .findFirst();

            assertThat(foreignTransaction).isPresent();
            assertThat(foreignTransaction.get().valueUsd()).isPositive();
        }

        @Test
        @DisplayName("Should parse expected total transaction count")
        void shouldParseExpectedTransactionCount() {
            var sections = pdfParserService.parseCardSections(pdfText);

            var totalTransactions = sections.stream()
                    .mapToInt(s -> s.transactions().size())
                    .sum();

            assertThat(totalTransactions).isGreaterThanOrEqualTo(150);
        }

        @Test
        @DisplayName("Should correctly normalize whitespace in transaction lines")
        void shouldNormalizeWhitespaceInTransactionLines() {
            var lineWithNbsp = "\u00A031/07/2025   FeFloresCostura 02/02    196,50   0,00\u00A0";

            var result = pdfParserService.parseTransactionLine(lineWithNbsp, "7152", "ALEXANDRE C VIEIRA");

            assertThat(result).isPresent();
            assertThat(result.get().date()).isEqualTo(LocalDate.of(2025, 7, 31));
            assertThat(result.get().description()).isEqualTo("FeFloresCostura 02/02");
        }

        @Test
        @DisplayName("Should parse transactions from different card holders")
        void shouldParseTransactionsFromDifferentCardHolders() {
            var sections = pdfParserService.parseCardSections(pdfText);

            var holderNames = sections.stream()
                    .map(ParsedCardSection::holderName)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            assertThat(holderNames).hasSizeGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("CSV Parsing with Real File")
    class CsvParsingTests {

        private List<String> csvLines;

        @BeforeEach
        void loadCsvLines() throws Exception {
            var resourceUrl = getClass().getClassLoader().getResource(TEST_DATA_PATH + CSV_FILE);
            assumeTrue(resourceUrl != null, "CSV test file not found in resources");

            csvLines = Files.readAllLines(Path.of(resourceUrl.toURI()), Charset.forName("ISO-8859-1"));
        }

        @Test
        @DisplayName("Should contain expected header metadata")
        void shouldContainExpectedHeaderMetadata() {
            assertThat(csvLines).isNotEmpty();

            var hasCardInfo = csvLines.stream().anyMatch(l -> l.contains("Nome do cart"));
            var hasColumnHeader = csvLines.stream().anyMatch(l -> l.startsWith("Data;Descri"));

            assertThat(hasCardInfo).isTrue();
            assertThat(hasColumnHeader).isTrue();
        }

        @Test
        @DisplayName("Should have transaction lines with correct format")
        void shouldHaveTransactionLinesWithCorrectFormat() {
            var transactionLines = csvLines.stream()
                    .filter(l -> l.matches("^\\d{2}/\\d{2}/\\d{4};.*"))
                    .toList();

            assertThat(transactionLines).isNotEmpty();
            assertThat(transactionLines.size()).isGreaterThanOrEqualTo(100);
        }

        @Test
        @DisplayName("Should contain installment transactions")
        void shouldContainInstallmentTransactions() {
            var installmentLines = csvLines.stream()
                    .filter(l -> l.matches(".*\\d{2}/\\d{2}[^/].*"))
                    .filter(l -> l.matches("^\\d{2}/\\d{2}/\\d{4};.*"))
                    .toList();

            assertThat(installmentLines).isNotEmpty();
        }

        @Test
        @DisplayName("Should contain payment with negative value")
        void shouldContainPaymentWithNegativeValue() {
            var paymentLine = csvLines.stream()
                    .filter(l -> l.contains("PGTO HOME/OFFICE BANKING"))
                    .findFirst();

            assertThat(paymentLine).isPresent();
            assertThat(paymentLine.get()).contains("-12.855,13");
        }

        @Test
        @DisplayName("Should contain foreign transactions with USD")
        void shouldContainForeignTransactionsWithUsd() {
            var foreignLines = csvLines.stream()
                    .filter(l -> l.matches("^\\d{2}/\\d{2}/\\d{4};.*"))
                    .filter(l -> {
                        var parts = l.split(";");
                        if (parts.length >= 4) {
                            try {
                                var usd = parts[3].replace(",", ".").trim();
                                return !usd.isEmpty() && Double.parseDouble(usd) > 0;
                            } catch (NumberFormatException e) {
                                return false;
                            }
                        }
                        return false;
                    })
                    .toList();

            assertThat(foreignLines).isNotEmpty();
            assertThat(foreignLines.stream().anyMatch(l -> l.contains("RENDER.COM"))).isTrue();
        }

        @Test
        @DisplayName("Should have exchange rate info lines that need to be skipped")
        void shouldHaveExchangeRateInfoLines() {
            var exchangeRateLines = csvLines.stream()
                    .filter(l -> l.startsWith(";USD"))
                    .toList();

            assertThat(exchangeRateLines).isNotEmpty();
        }

        @Test
        @DisplayName("Should have multiple card sections")
        void shouldHaveMultipleCardSections() {
            var cardHeaders = csvLines.stream()
                    .filter(l -> l.matches("^\\d{4} - [A-Z ]+$"))
                    .toList();

            assertThat(cardHeaders).hasSizeGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("Should have summary/footer lines that need to be skipped")
        void shouldHaveSummaryFooterLines() {
            var summaryLines = csvLines.stream()
                    .filter(l -> l.contains("Saldo da fatura anterior") ||
                            l.contains("Total da fatura") ||
                            l.contains("Pagamento m"))
                    .toList();

            assertThat(summaryLines).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Cross-Format Checksum Consistency")
    class CrossFormatChecksumTests {

        private String pdfText;
        private List<String> csvLines;

        @BeforeEach
        void loadFiles() throws Exception {
            var pdfStream = getClass().getClassLoader().getResourceAsStream(TEST_DATA_PATH + PDF_FILE);
            var csvUrl = getClass().getClassLoader().getResource(TEST_DATA_PATH + CSV_FILE);

            assumeTrue(pdfStream != null && csvUrl != null, "Test files not found");

            try (var document = Loader.loadPDF(pdfStream.readAllBytes())) {
                pdfText = new PDFTextStripper().getText(document);
            }
            csvLines = Files.readAllLines(Path.of(csvUrl.toURI()), Charset.forName("ISO-8859-1"));
        }

        @Test
        @DisplayName("Should generate same normalized checksum for matching PDF and CSV transactions")
        void shouldGenerateSameNormalizedChecksumForMatchingTransactions() {
            var sections = pdfParserService.parseCardSections(pdfText);

            var pdfFeFlores = sections.stream()
                    .flatMap(s -> s.transactions().stream())
                    .filter(t -> t.description().contains("FeFloresCostura") && t.description().contains("02/02"))
                    .findFirst();

            assertThat(pdfFeFlores).isPresent();

            var csvFeFloresLine = csvLines.stream()
                    .filter(l -> l.contains("FeFloresCostura") && l.contains("02/02"))
                    .filter(l -> l.startsWith("31/07/2025"))
                    .findFirst();

            assertThat(csvFeFloresLine).isPresent();

            var pdfChecksum = NormalizationUtils.generateNormalizedChecksum(
                    pdfFeFlores.get().date(),
                    pdfFeFlores.get().description(),
                    pdfFeFlores.get().valueBrl(),
                    1L
            );

            var csvParts = csvFeFloresLine.get().split(";");
            var csvDate = LocalDate.of(2025, 7, 31);
            var csvDescription = csvParts[1].trim();
            var csvValue = new BigDecimal(csvParts[2].replace(".", "").replace(",", ".").trim());

            var csvChecksum = NormalizationUtils.generateNormalizedChecksum(
                    csvDate,
                    csvDescription,
                    csvValue,
                    1L
            );

            assertThat(pdfChecksum).isEqualTo(csvChecksum);
        }

        @Test
        @DisplayName("Should generate same normalized checksum for payment transaction")
        void shouldGenerateSameNormalizedChecksumForPayment() {
            var sections = pdfParserService.parseCardSections(pdfText);

            var pdfPayment = sections.stream()
                    .flatMap(s -> s.transactions().stream())
                    .filter(t -> t.description().contains("PGTO HOME/OFFICE BANKING"))
                    .findFirst();

            var csvPaymentLine = csvLines.stream()
                    .filter(l -> l.contains("PGTO HOME/OFFICE BANKING"))
                    .filter(l -> l.startsWith("01/09/2025"))
                    .findFirst();

            assumeTrue(pdfPayment.isPresent() && csvPaymentLine.isPresent(), "Payment transactions not found");

            var pdfChecksum = NormalizationUtils.generateNormalizedChecksum(
                    pdfPayment.get().date(),
                    pdfPayment.get().description(),
                    pdfPayment.get().valueBrl(),
                    1L
            );

            var csvParts = csvPaymentLine.get().split(";");
            var csvDate = LocalDate.of(2025, 9, 1);
            var csvDescription = csvParts[1].trim();
            var csvValue = new BigDecimal(csvParts[2].replace(".", "").replace(",", ".").trim());

            var csvChecksum = NormalizationUtils.generateNormalizedChecksum(
                    csvDate,
                    csvDescription,
                    csvValue,
                    1L
            );

            assertThat(pdfChecksum).isEqualTo(csvChecksum);
        }

        @Test
        @DisplayName("Should handle whitespace differences between formats")
        void shouldHandleWhitespaceDifferencesBetweenFormats() {
            var pdfDescription = "FeFloresCostura 02/02";
            var csvDescription = "FeFloresCostura     02/02               ";

            var date = LocalDate.of(2025, 7, 31);
            var value = new BigDecimal("196.50");
            var accountId = 1L;

            var pdfChecksum = NormalizationUtils.generateNormalizedChecksum(date, pdfDescription, value, accountId);
            var csvChecksum = NormalizationUtils.generateNormalizedChecksum(date, csvDescription, value, accountId);

            assertThat(pdfChecksum).isEqualTo(csvChecksum);
        }
    }

    @Nested
    @DisplayName("Edge Case Handling")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty lines gracefully")
        void shouldHandleEmptyLinesGracefully() {
            var result = pdfParserService.parseTransactionLine("", "7152", "TEST");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle card header lines")
        void shouldHandleCardHeaderLines() {
            var result = pdfParserService.parseTransactionLine("7152 - ALEXANDRE C VIEIRA", "7152", "TEST");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle lines with only whitespace")
        void shouldHandleLinesWithOnlyWhitespace() {
            var result = pdfParserService.parseTransactionLine("   \u00A0   ", "7152", "TEST");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle very long descriptions")
        void shouldHandleVeryLongDescriptions() {
            var longLine = "31/07/2025   VERY LONG DESCRIPTION THAT GOES ON AND ON PORTO ALEGRE BRA    196,50   0,00";

            var result = pdfParserService.parseTransactionLine(longLine, "7152", "TEST");

            assertThat(result).isPresent();
            assertThat(result.get().description()).contains("VERY LONG DESCRIPTION");
        }

        @Test
        @DisplayName("Should handle small decimal values")
        void shouldHandleSmallDecimalValues() {
            var line = "02/09/2025   IOF ADICIONAL SOBRE PAGAMENTO CONTAS    0,48   0,00";

            var result = pdfParserService.parseTransactionLine(line, "7152", "TEST");

            assertThat(result).isPresent();
            assertThat(result.get().valueBrl()).isEqualByComparingTo(new BigDecimal("0.48"));
        }

        @Test
        @DisplayName("Should handle large decimal values with thousands separator")
        void shouldHandleLargeDecimalValuesWithThousandsSeparator() {
            var line = "01/09/2025   PGTO HOME/OFFICE BANKING    -12.855,13   0,00";

            var result = pdfParserService.parseTransactionLine(line, "7152", "TEST");

            assertThat(result).isPresent();
            assertThat(result.get().valueBrl()).isEqualByComparingTo(new BigDecimal("-12855.13"));
        }

        @Test
        @DisplayName("Should parse installment patterns correctly")
        void shouldParseInstallmentPatternsCorrectly() {
            var testCases = List.of(
                    new String[]{"31/07/2025   FeFloresCostura 01/02    196,50   0,00", "1", "2"},
                    new String[]{"31/07/2025   FeFloresCostura 02/02    196,50   0,00", "2", "2"},
                    new String[]{"27/11/2024   MP *KIT 10/10    35,59   0,00", "10", "10"},
                    new String[]{"17/09/2025   CENTAURO 01/02 PORTO ALEGREBR    254,99   0,00", "1", "2"}
            );

            for (var testCase : testCases) {
                var result = pdfParserService.parseTransactionLine(testCase[0], "7152", "TEST");
                assertThat(result).as("Line: " + testCase[0]).isPresent();
                assertThat(result.get().currentInstallment()).as("Current installment for: " + testCase[0])
                        .isEqualTo(Integer.parseInt(testCase[1]));
                assertThat(result.get().totalInstallments()).as("Total installments for: " + testCase[0])
                        .isEqualTo(Integer.parseInt(testCase[2]));
            }
        }

        @Test
        @DisplayName("Should not extract installments from dates")
        void shouldNotExtractInstallmentsFromDates() {
            var line = "26/08/2025   ADHomeMarketLtda PORTO ALEGRE BRA    25,97   0,00";

            var result = pdfParserService.parseTransactionLine(line, "7152", "TEST");

            assertThat(result).isPresent();
            assertThat(result.get().currentInstallment()).isNull();
            assertThat(result.get().totalInstallments()).isNull();
        }
    }
}
