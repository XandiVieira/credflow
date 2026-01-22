package com.relyon.credflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.relyon.credflow.exception.PdfProcessingException;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.CreditCardRepository;
import com.relyon.credflow.repository.DescriptionMappingRepository;
import com.relyon.credflow.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class BanrisulPdfParserServiceTest {

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

    @InjectMocks
    private BanrisulPdfParserService service;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder().id(1L).build();
    }

    @Nested
    class ParseBrazilianDecimal {

        @Test
        void shouldParsePositiveDecimalWithThousandsSeparator() {
            var result = service.parseBrazilianDecimal("1.234,56");
            assertThat(result).isEqualByComparingTo(new BigDecimal("1234.56"));
        }

        @Test
        void shouldParseSimpleDecimal() {
            var result = service.parseBrazilianDecimal("123,45");
            assertThat(result).isEqualByComparingTo(new BigDecimal("123.45"));
        }

        @Test
        void shouldParseNegativeDecimal() {
            var result = service.parseBrazilianDecimal("-12.855,13");
            assertThat(result).isEqualByComparingTo(new BigDecimal("-12855.13"));
        }

        @Test
        void shouldParseZeroDecimal() {
            var result = service.parseBrazilianDecimal("0,00");
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldParseLargeNumber() {
            var result = service.parseBrazilianDecimal("100.000,99");
            assertThat(result).isEqualByComparingTo(new BigDecimal("100000.99"));
        }
    }

    @Nested
    class ParseTransactionLine {

        @Test
        void shouldParseBasicTransactionLine() {
            var line = "31/07/2025 FeFloresCostura 02/02 196,50 0,00";

            var result = service.parseTransactionLine(line, "7152", "ALEXANDRE C VIEIRA");

            assertThat(result).isPresent();
            var tx = result.get();
            assertThat(tx.date()).isEqualTo(LocalDate.of(2025, 7, 31));
            assertThat(tx.description()).isEqualTo("FeFloresCostura 02/02");
            assertThat(tx.valueBrl()).isEqualByComparingTo(new BigDecimal("196.50"));
            assertThat(tx.valueUsd()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(tx.currentInstallment()).isEqualTo(2);
            assertThat(tx.totalInstallments()).isEqualTo(2);
            assertThat(tx.cardLastFourDigits()).isEqualTo("7152");
            assertThat(tx.cardHolderName()).isEqualTo("ALEXANDRE C VIEIRA");
        }

        @Test
        void shouldParsePaymentWithNegativeValue() {
            var line = "01/09/2025 PGTO HOME/OFFICE BANKING -12.855,13 0,00";

            var result = service.parseTransactionLine(line, "7152", "ALEXANDRE C VIEIRA");

            assertThat(result).isPresent();
            var tx = result.get();
            assertThat(tx.valueBrl()).isEqualByComparingTo(new BigDecimal("-12855.13"));
        }

        @Test
        void shouldParseTransactionWithoutInstallment() {
            var line = "26/08/2025 ADHomeMarketLtda PORTO ALEGRE BRA 25,97 0,00";

            var result = service.parseTransactionLine(line, "7152", "ALEXANDRE C VIEIRA");

            assertThat(result).isPresent();
            var tx = result.get();
            assertThat(tx.description()).isEqualTo("ADHomeMarketLtda PORTO ALEGRE BRA");
            assertThat(tx.currentInstallment()).isNull();
            assertThat(tx.totalInstallments()).isNull();
        }

        @Test
        void shouldParseTransactionWithUsdValue() {
            var line = "03/09/2025 RENDER.COM SAN FRANCISCO CA 214,02 38,00";

            var result = service.parseTransactionLine(line, "7928", "ALEXANDRE C VIEIRA");

            assertThat(result).isPresent();
            var tx = result.get();
            assertThat(tx.valueBrl()).isEqualByComparingTo(new BigDecimal("214.02"));
            assertThat(tx.valueUsd()).isEqualByComparingTo(new BigDecimal("38.00"));
        }

        @Test
        void shouldReturnEmptyForInvalidLine() {
            var line = "This is not a valid transaction line";

            var result = service.parseTransactionLine(line, "7152", "ALEXANDRE C VIEIRA");

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmptyForCardHeader() {
            var line = "7152 - ALEXANDRE C VIEIRA";

            var result = service.parseTransactionLine(line, "7152", "ALEXANDRE C VIEIRA");

            assertThat(result).isEmpty();
        }

        @Test
        void shouldParseLineWithLeadingSpacesAndMultipleSpacesBetweenColumns() {
            var line = " 31/07/2025   FeFloresCostura 02/02    196,50   0,00 ";

            var result = service.parseTransactionLine(line.trim(), "7152", "ALEXANDRE C VIEIRA");

            assertThat(result).isPresent();
            var tx = result.get();
            assertThat(tx.date()).isEqualTo(LocalDate.of(2025, 7, 31));
            assertThat(tx.description()).isEqualTo("FeFloresCostura 02/02");
            assertThat(tx.valueBrl()).isEqualByComparingTo(new BigDecimal("196.50"));
            assertThat(tx.currentInstallment()).isEqualTo(2);
            assertThat(tx.totalInstallments()).isEqualTo(2);
        }

        @Test
        void shouldParsePaymentLineWithThousandsSeparator() {
            var line = " 01/09/2025   PGTO HOME/OFFICE BANKING    -12.855,13   0,00 ";

            var result = service.parseTransactionLine(line.trim(), "7152", "ALEXANDRE C VIEIRA");

            assertThat(result).isPresent();
            var tx = result.get();
            assertThat(tx.valueBrl()).isEqualByComparingTo(new BigDecimal("-12855.13"));
        }

        @Test
        void shouldParseLineWithExactPdfFormat() {
            // Exact format from actual PDF extraction
            var line = "31/07/2025   FeFloresCostura 02/02    196,50   0,00";

            var result = service.parseTransactionLine(line, "7152", "ALEXANDRE C VIEIRA");

            assertThat(result).isPresent();
            var tx = result.get();
            assertThat(tx.date()).isEqualTo(LocalDate.of(2025, 7, 31));
            assertThat(tx.valueBrl()).isEqualByComparingTo(new BigDecimal("196.50"));
        }

        @Test
        void shouldParseLineWithLongDescriptionAndLocation() {
            // Example from actual PDF
            var line = "26/08/2025   ADHomeMarketLtda PORTO ALEGRE BRA    25,97   0,00";

            var result = service.parseTransactionLine(line, "7152", "ALEXANDRE C VIEIRA");

            assertThat(result).isPresent();
            var tx = result.get();
            assertThat(tx.description()).isEqualTo("ADHomeMarketLtda PORTO ALEGRE BRA");
            assertThat(tx.valueBrl()).isEqualByComparingTo(new BigDecimal("25.97"));
        }

        @Test
        void shouldParseLastInstallment() {
            var line = "27/11/2024 MP *KIT 10/10 35,59 0,00";

            var result = service.parseTransactionLine(line, "7928", "ALEXANDRE C VIEIRA");

            assertThat(result).isPresent();
            var tx = result.get();
            assertThat(tx.currentInstallment()).isEqualTo(10);
            assertThat(tx.totalInstallments()).isEqualTo(10);
        }

        @Test
        void shouldHandleNonBreakingSpaceAtStartAndEnd() {
            var line = "\u00A031/07/2025   FeFloresCostura 02/02    196,50   0,00\u00A0";

            var result = service.parseTransactionLine(line, "7152", "ALEXANDRE C VIEIRA");

            assertThat(result).isPresent();
            var tx = result.get();
            assertThat(tx.date()).isEqualTo(LocalDate.of(2025, 7, 31));
            assertThat(tx.description()).isEqualTo("FeFloresCostura 02/02");
            assertThat(tx.valueBrl()).isEqualByComparingTo(new BigDecimal("196.50"));
        }

        @Test
        void shouldHandleLineWithOnlyNbspWhitespace() {
            var line = "\u00A0 26/08/2025   ADHomeMarketLtda PORTO ALEGRE BRA    25,97   0,00 \u00A0";

            var result = service.parseTransactionLine(line, "7152", "ALEXANDRE C VIEIRA");

            assertThat(result).isPresent();
            var tx = result.get();
            assertThat(tx.description()).isEqualTo("ADHomeMarketLtda PORTO ALEGRE BRA");
        }
    }

    @Nested
    class NormalizeWhitespace {

        @Test
        void shouldReplaceNbspWithRegularSpace() {
            var input = "\u00A0text\u00A0";

            var result = service.normalizeWhitespace(input);

            assertThat(result).isEqualTo("text");
        }

        @Test
        void shouldReplaceNarrowNoBreakSpace() {
            var input = "\u202Ftext\u202F";

            var result = service.normalizeWhitespace(input);

            assertThat(result).isEqualTo("text");
        }

        @Test
        void shouldReplaceFigureSpace() {
            var input = "\u2007text\u2007";

            var result = service.normalizeWhitespace(input);

            assertThat(result).isEqualTo("text");
        }

        @Test
        void shouldTrimRegularSpaces() {
            var input = "  text  ";

            var result = service.normalizeWhitespace(input);

            assertThat(result).isEqualTo("text");
        }

        @Test
        void shouldHandleMixedWhitespace() {
            var input = "\u00A0 31/07/2025   text   196,50 \u00A0";

            var result = service.normalizeWhitespace(input);

            assertThat(result).isEqualTo("31/07/2025   text   196,50");
        }
    }

    @Nested
    class ParseCardSections {

        @Test
        void shouldParseSingleCardSection() {
            var pdfText = """
                    7152 - ALEXANDRE C VIEIRA
                    31/07/2025 FeFloresCostura 02/02 196,50 0,00
                    26/08/2025 ADHomeMarketLtda PORTO ALEGRE BRA 25,97 0,00
                    """;

            var result = service.parseCardSections(pdfText);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().lastFourDigits()).isEqualTo("7152");
            assertThat(result.getFirst().holderName()).isEqualTo("ALEXANDRE C VIEIRA");
            assertThat(result.getFirst().transactions()).hasSize(2);
        }

        @Test
        void shouldParseMultipleCardSections() {
            var pdfText = """
                    7152 - ALEXANDRE C VIEIRA
                    31/07/2025 FeFloresCostura 02/02 196,50 0,00
                    
                    7928 - ALEXANDRE C VIEIRA
                    27/11/2024 MP *KIT 10/10 35,59 0,00
                    
                    3294 - POLYANA FUCILINI
                    09/12/2024 DM*hostingercomb 10/12 12,42 0,00
                    """;

            var result = service.parseCardSections(pdfText);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).lastFourDigits()).isEqualTo("7152");
            assertThat(result.get(1).lastFourDigits()).isEqualTo("7928");
            assertThat(result.get(2).lastFourDigits()).isEqualTo("3294");
            assertThat(result.get(2).holderName()).isEqualTo("POLYANA FUCILINI");
        }

        @Test
        void shouldIgnoreNonTransactionLines() {
            var pdfText = """
                    7152 - ALEXANDRE C VIEIRA
                    31/07/2025 FeFloresCostura 02/02 196,50 0,00
                    Lançamentos no Brasil
                    Saldo da fatura anterior R$ 12.855,13
                    """;

            var result = service.parseCardSections(pdfText);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().transactions()).hasSize(1);
        }

        @Test
        void shouldReturnEmptyListForEmptyText() {
            var result = service.parseCardSections("");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class FileValidation {

        @Test
        void shouldThrowExceptionForEmptyFile() {
            var emptyFile = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[0]);

            assertThatThrownBy(() -> service.importFromPdf(emptyFile, 1L))
                    .isInstanceOf(PdfProcessingException.class);
        }

        @Test
        void shouldThrowExceptionForInvalidContentType() {
            var csvFile = new MockMultipartFile("file", "test.csv", "text/csv", "data".getBytes());

            assertThatThrownBy(() -> service.importFromPdf(csvFile, 1L))
                    .isInstanceOf(PdfProcessingException.class);
        }

        @Test
        void shouldThrowExceptionForInvalidExtension() {
            var wrongExtension = new MockMultipartFile("file", "test.txt", "application/pdf", "data".getBytes());

            assertThatThrownBy(() -> service.importFromPdf(wrongExtension, 1L))
                    .isInstanceOf(PdfProcessingException.class);
        }
    }

    @Nested
    class ImportFromPdf {

        @Test
        void shouldSkipDuplicateTransactionsByChecksum() {
            when(accountService.findById(1L)).thenReturn(testAccount);
            when(mappingRepository.findAllByAccountId(1L)).thenReturn(Collections.emptyList());
            when(creditCardRepository.findByLastFourDigitsAndAccountId(any(), eq(1L)))
                    .thenReturn(Collections.emptyList());
            when(transactionRepository.existsByChecksum(any())).thenReturn(true);

            var service = spy(BanrisulPdfParserServiceTest.this.service);
            doReturn("""
                    7152 - ALEXANDRE C VIEIRA
                    31/07/2025 FeFloresCostura 02/02 196,50 0,00
                    """).when(service).extractTextFromPdf(any());

            var pdfFile = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy".getBytes());
            var result = service.importFromPdf(pdfFile, 1L);

            assertThat(result).isEmpty();
            verify(transactionRepository, never()).save(any());
        }

        @Test
        void shouldResolveCreditCardByLastFourDigits() {
            var creditCard = CreditCard.builder()
                    .id(1L)
                    .lastFourDigits("7152")
                    .holder(User.builder().name("Alexandre Vieira").build())
                    .build();

            when(accountService.findById(1L)).thenReturn(testAccount);
            when(mappingRepository.findAllByAccountId(1L)).thenReturn(Collections.emptyList());
            when(creditCardRepository.findByLastFourDigitsAndAccountId("7152", 1L))
                    .thenReturn(List.of(creditCard));
            when(transactionRepository.existsByChecksum(any())).thenReturn(false);
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var service = spy(BanrisulPdfParserServiceTest.this.service);
            doReturn("""
                    7152 - ALEXANDRE C VIEIRA
                    31/07/2025 FeFloresCostura 02/02 196,50 0,00
                    """).when(service).extractTextFromPdf(any());

            var pdfFile = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy".getBytes());
            var result = service.importFromPdf(pdfFile, 1L);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getCreditCard()).isEqualTo(creditCard);
        }

        @Test
        void shouldCreateNewMappingsForUnknownDescriptions() {
            when(accountService.findById(1L)).thenReturn(testAccount);
            when(mappingRepository.findAllByAccountId(1L)).thenReturn(Collections.emptyList());
            when(creditCardRepository.findByLastFourDigitsAndAccountId(any(), eq(1L)))
                    .thenReturn(Collections.emptyList());
            when(transactionRepository.existsByChecksum(any())).thenReturn(false);
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var service = spy(BanrisulPdfParserServiceTest.this.service);
            doReturn("""
                    7152 - ALEXANDRE C VIEIRA
                    31/07/2025 FeFloresCostura 02/02 196,50 0,00
                    """).when(service).extractTextFromPdf(any());

            var pdfFile = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy".getBytes());
            service.importFromPdf(pdfFile, 1L);

            verify(mappingRepository).saveAll(anyCollection());
        }

        @Test
        void shouldReuseExistingMappings() {
            var existingMapping = DescriptionMapping.builder()
                    .normalizedDescription("feflorescostura")
                    .simplifiedDescription("Flores Costura")
                    .account(testAccount)
                    .build();

            when(accountService.findById(1L)).thenReturn(testAccount);
            when(mappingRepository.findAllByAccountId(1L)).thenReturn(List.of(existingMapping));
            when(creditCardRepository.findByLastFourDigitsAndAccountId(any(), eq(1L)))
                    .thenReturn(Collections.emptyList());
            when(transactionRepository.existsByChecksum(any())).thenReturn(false);
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var service = spy(BanrisulPdfParserServiceTest.this.service);
            doReturn("""
                    7152 - ALEXANDRE C VIEIRA
                    31/07/2025 FeFloresCostura 02/02 196,50 0,00
                    """).when(service).extractTextFromPdf(any());

            var pdfFile = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy".getBytes());
            var result = service.importFromPdf(pdfFile, 1L);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getSimplifiedDescription()).isEqualTo("Flores Costura");
        }

        @Test
        void shouldNegatePositiveValuesAsExpenses() {
            when(accountService.findById(1L)).thenReturn(testAccount);
            when(mappingRepository.findAllByAccountId(1L)).thenReturn(Collections.emptyList());
            when(creditCardRepository.findByLastFourDigitsAndAccountId(any(), eq(1L)))
                    .thenReturn(Collections.emptyList());
            when(transactionRepository.existsByChecksum(any())).thenReturn(false);
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var service = spy(BanrisulPdfParserServiceTest.this.service);
            doReturn("""
                    7152 - ALEXANDRE C VIEIRA
                    31/07/2025 FeFloresCostura 02/02 196,50 0,00
                    """).when(service).extractTextFromPdf(any());

            var pdfFile = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy".getBytes());
            var result = service.importFromPdf(pdfFile, 1L);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getValue()).isEqualByComparingTo(new BigDecimal("-196.50"));
        }

        @Test
        void shouldKeepNegativeValuesAsPayments() {
            when(accountService.findById(1L)).thenReturn(testAccount);
            when(mappingRepository.findAllByAccountId(1L)).thenReturn(Collections.emptyList());
            when(creditCardRepository.findByLastFourDigitsAndAccountId(any(), eq(1L)))
                    .thenReturn(Collections.emptyList());
            when(transactionRepository.existsByChecksum(any())).thenReturn(false);
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var service = spy(BanrisulPdfParserServiceTest.this.service);
            doReturn("""
                    7152 - ALEXANDRE C VIEIRA
                    01/09/2025 PGTO HOME/OFFICE BANKING -12.855,13 0,00
                    """).when(service).extractTextFromPdf(any());

            var pdfFile = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy".getBytes());
            var result = service.importFromPdf(pdfFile, 1L);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getValue()).isEqualByComparingTo(new BigDecimal("-12855.13"));
        }

        @Test
        void shouldRunRefundDetectionOnImportedTransactions() {
            when(accountService.findById(1L)).thenReturn(testAccount);
            when(mappingRepository.findAllByAccountId(1L)).thenReturn(Collections.emptyList());
            when(creditCardRepository.findByLastFourDigitsAndAccountId(any(), eq(1L)))
                    .thenReturn(Collections.emptyList());
            when(transactionRepository.existsByChecksum(any())).thenReturn(false);
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var service = spy(BanrisulPdfParserServiceTest.this.service);
            doReturn("""
                    7152 - ALEXANDRE C VIEIRA
                    31/07/2025 FeFloresCostura 02/02 196,50 0,00
                    """).when(service).extractTextFromPdf(any());

            var pdfFile = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy".getBytes());
            var result = service.importFromPdf(pdfFile, 1L);

            verify(refundDetectionService).detectAndLinkReversal(result.getFirst());
        }
    }

    @Nested
    class FuzzyNameMatching {

        @Test
        void shouldMatchCardByFuzzyNameWhenMultipleCandidates() {
            var card1 = CreditCard.builder()
                    .id(1L)
                    .lastFourDigits("7152")
                    .holder(User.builder().name("Alexandre Vieira").build())
                    .build();

            var card2 = CreditCard.builder()
                    .id(2L)
                    .lastFourDigits("7152")
                    .holder(User.builder().name("Polyana Fucilini").build())
                    .build();

            when(accountService.findById(1L)).thenReturn(testAccount);
            when(mappingRepository.findAllByAccountId(1L)).thenReturn(Collections.emptyList());
            when(creditCardRepository.findByLastFourDigitsAndAccountId("7152", 1L))
                    .thenReturn(List.of(card1, card2));
            when(transactionRepository.existsByChecksum(any())).thenReturn(false);
            when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            var service = spy(BanrisulPdfParserServiceTest.this.service);
            doReturn("""
                    7152 - ALEXANDRE C VIEIRA
                    31/07/2025 FeFloresCostura 02/02 196,50 0,00
                    """).when(service).extractTextFromPdf(any());

            var pdfFile = new MockMultipartFile("file", "test.pdf", "application/pdf", "dummy".getBytes());
            var result = service.importFromPdf(pdfFile, 1L);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getCreditCard()).isEqualTo(card1);
        }
    }
}
