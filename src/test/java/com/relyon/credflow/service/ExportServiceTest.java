package com.relyon.credflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.transaction.TransactionFilter;
import com.relyon.credflow.model.transaction.TransactionType;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.TransactionRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ExportService exportService;

    private TransactionFilter filter;
    private List<Transaction> transactions;

    @BeforeEach
    void setUp() {
        filter = new TransactionFilter(
                1L, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31),
                null, null, null, null, null, null, null, null, null, null
        );

        var account = new Account();
        account.setId(1L);

        var category = new Category();
        category.setName("Food");

        var creditCard = new CreditCard();
        creditCard.setNickname("Main Card");

        var user = new User();
        user.setId(1L);
        user.setName("John Doe");

        var tx1 = new Transaction();
        tx1.setDate(LocalDate.of(2025, 1, 15));
        tx1.setDescription("Restaurant Lunch");
        tx1.setValue(new BigDecimal("-50.00"));
        tx1.setCategory(category);
        tx1.setCreditCard(creditCard);
        tx1.setResponsibleUsers(Set.of(user));
        tx1.setTransactionType(TransactionType.ONE_TIME);
        tx1.setAccount(account);

        var tx2 = new Transaction();
        tx2.setDate(LocalDate.of(2025, 1, 20));
        tx2.setDescription("Salary");
        tx2.setValue(new BigDecimal("3000.00"));
        tx2.setCategory(null);
        tx2.setCreditCard(null);
        tx2.setResponsibleUsers(Set.of());
        tx2.setTransactionType(TransactionType.PAYMENT);
        tx2.setAccount(account);

        transactions = List.of(tx1, tx2);
    }

    @Nested
    class ExportToCsv {

        @Test
        @SuppressWarnings("unchecked")
        void shouldGenerateCsvWithHeader() {
            when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(transactions);

            var result = exportService.exportToCsv(filter);
            var csv = new String(result);

            assertThat(csv).startsWith("Date,Description,Category,Responsible Users,Credit Card,Value,Type\n");
        }

        @Test
        @SuppressWarnings("unchecked")
        void shouldIncludeAllTransactionData() {
            when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(transactions);

            var result = exportService.exportToCsv(filter);
            var csv = new String(result);

            assertThat(csv).contains("15/01/2025");
            assertThat(csv).contains("Restaurant Lunch");
            assertThat(csv).contains("Food");
            assertThat(csv).contains("John Doe");
            assertThat(csv).contains("Main Card");
            assertThat(csv).contains("-50.00");
            assertThat(csv).contains("ONE_TIME");
        }

        @Test
        @SuppressWarnings("unchecked")
        void shouldHandleNullCategoryAndCreditCard() {
            when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(transactions);

            var result = exportService.exportToCsv(filter);
            var csv = new String(result);

            assertThat(csv).contains("Salary,,,,3000.00,PAYMENT");
        }

        @Test
        @SuppressWarnings("unchecked")
        void shouldEscapeCsvSpecialCharacters() {
            var tx = new Transaction();
            tx.setDate(LocalDate.of(2025, 1, 1));
            tx.setDescription("Item, with \"quotes\" and comma");
            tx.setValue(BigDecimal.TEN);
            tx.setTransactionType(TransactionType.ONE_TIME);

            when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(List.of(tx));

            var result = exportService.exportToCsv(filter);
            var csv = new String(result);

            assertThat(csv).contains("\"Item, with \"\"quotes\"\" and comma\"");
        }

        @Test
        @SuppressWarnings("unchecked")
        void shouldReturnEmptyCsvWhenNoTransactions() {
            when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(List.of());

            var result = exportService.exportToCsv(filter);
            var csv = new String(result);

            assertThat(csv).isEqualTo("Date,Description,Category,Responsible Users,Credit Card,Value,Type\n");
        }
    }

    @Nested
    class ExportToPdf {

        @Test
        @SuppressWarnings("unchecked")
        void shouldGenerateNonEmptyPdf() throws IOException {
            when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(transactions);

            var result = exportService.exportToPdf(filter);

            assertThat(result).isNotEmpty();
            assertThat(result[0]).isEqualTo((byte) '%');
            assertThat(result[1]).isEqualTo((byte) 'P');
            assertThat(result[2]).isEqualTo((byte) 'D');
            assertThat(result[3]).isEqualTo((byte) 'F');
        }

        @Test
        @SuppressWarnings("unchecked")
        void shouldGeneratePdfWithEmptyTransactions() throws IOException {
            when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(List.of());

            var result = exportService.exportToPdf(filter);

            assertThat(result).isNotEmpty();
        }
    }

    @Nested
    class ExportToExcel {

        @Test
        @SuppressWarnings("unchecked")
        void shouldGenerateValidExcelFile() throws IOException {
            when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(transactions);

            var result = exportService.exportToExcel(filter);

            try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
                var sheet = workbook.getSheet("Transactions");
                assertThat(sheet).isNotNull();
            }
        }

        @Test
        @SuppressWarnings("unchecked")
        void shouldIncludeHeaderRow() throws IOException {
            when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(transactions);

            var result = exportService.exportToExcel(filter);

            try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
                var sheet = workbook.getSheet("Transactions");
                var headerRow = sheet.getRow(0);

                assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("Date");
                assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("Description");
                assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("Category");
                assertThat(headerRow.getCell(5).getStringCellValue()).isEqualTo("Value");
            }
        }

        @Test
        @SuppressWarnings("unchecked")
        void shouldIncludeTransactionData() throws IOException {
            when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(transactions);

            var result = exportService.exportToExcel(filter);

            try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
                var sheet = workbook.getSheet("Transactions");

                assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Restaurant Lunch");
                assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("Food");
                assertThat(sheet.getRow(1).getCell(5).getNumericCellValue()).isEqualTo(-50.00);
            }
        }

        @Test
        @SuppressWarnings("unchecked")
        void shouldIncludeSummarySection() throws IOException {
            when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(transactions);

            var result = exportService.exportToExcel(filter);

            try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
                var sheet = workbook.getSheet("Transactions");

                var incomeRow = sheet.getRow(5);
                var expenseRow = sheet.getRow(6);
                var balanceRow = sheet.getRow(7);

                assertThat(incomeRow.getCell(0).getStringCellValue()).isEqualTo("Total Income:");
                assertThat(incomeRow.getCell(1).getNumericCellValue()).isEqualTo(3000.00);

                assertThat(expenseRow.getCell(0).getStringCellValue()).isEqualTo("Total Expense:");
                assertThat(expenseRow.getCell(1).getNumericCellValue()).isEqualTo(50.00);

                assertThat(balanceRow.getCell(0).getStringCellValue()).isEqualTo("Balance:");
                assertThat(balanceRow.getCell(1).getNumericCellValue()).isEqualTo(2950.00);
            }
        }

        @Test
        @SuppressWarnings("unchecked")
        void shouldHandleEmptyTransactions() throws IOException {
            when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(List.of());

            var result = exportService.exportToExcel(filter);

            try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
                var sheet = workbook.getSheet("Transactions");
                assertThat(sheet.getLastRowNum()).isGreaterThanOrEqualTo(0);
            }
        }

        @Test
        @SuppressWarnings("unchecked")
        void shouldHandleMultipleResponsibleUsers() throws IOException {
            var user1 = new User();
            user1.setName("Alice");
            var user2 = new User();
            user2.setName("Bob");

            var tx = new Transaction();
            tx.setDate(LocalDate.of(2025, 1, 1));
            tx.setDescription("Shared Expense");
            tx.setValue(new BigDecimal("-100.00"));
            tx.setResponsibleUsers(Set.of(user1, user2));
            tx.setTransactionType(TransactionType.ONE_TIME);

            when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                    .thenReturn(List.of(tx));

            var result = exportService.exportToExcel(filter);

            try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
                var sheet = workbook.getSheet("Transactions");
                var usersCell = sheet.getRow(1).getCell(3).getStringCellValue();

                assertThat(usersCell).contains("Alice");
                assertThat(usersCell).contains("Bob");
            }
        }
    }
}
