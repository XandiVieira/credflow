package com.relyon.credflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.transaction.TransactionFilter;
import com.relyon.credflow.model.transaction.TransactionSource;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AdvancedExcelExportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AdvancedExcelExportService service;

    @Test
    void exportToExcel_generatesValidExcelFile() throws IOException {
        var filter = createFilter();
        var transactions = createSampleTransactions();

        when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(transactions);

        var result = service.exportToExcel(filter);

        assertNotNull(result);
        assertTrue(result.length > 0);

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertNotNull(workbook);
            assertTrue(workbook.getNumberOfSheets() >= 7);
        }
    }

    @Test
    void exportToExcel_containsAllRequiredSheets() throws IOException {
        var filter = createFilter();
        var transactions = createSampleTransactions();

        when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(transactions);

        var result = service.exportToExcel(filter);

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertNotNull(workbook.getSheet("Dashboard"));
            assertNotNull(workbook.getSheet("Transações"));
            assertNotNull(workbook.getSheet("Por Categoria"));
            assertNotNull(workbook.getSheet("Por Mês"));
            assertNotNull(workbook.getSheet("Por Cartão"));
            assertNotNull(workbook.getSheet("Por Responsável"));
            assertNotNull(workbook.getSheet("Tendência Diária"));
        }
    }

    @Test
    void exportToExcel_transactionsSheetContainsData() throws IOException {
        var filter = createFilter();
        var transactions = createSampleTransactions();

        when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(transactions);

        var result = service.exportToExcel(filter);

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            var sheet = workbook.getSheet("Transações");
            assertNotNull(sheet);

            var headerRow = sheet.getRow(0);
            assertEquals("Data", headerRow.getCell(0).getStringCellValue());
            assertEquals("Descrição", headerRow.getCell(1).getStringCellValue());

            assertTrue(sheet.getLastRowNum() >= transactions.size());
        }
    }

    @Test
    void exportToExcel_dashboardContainsKPIs() throws IOException {
        var filter = createFilter();
        var transactions = createSampleTransactions();

        when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(transactions);

        var result = service.exportToExcel(filter);

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            var dashboard = workbook.getSheet("Dashboard");
            assertNotNull(dashboard);

            var titleRow = dashboard.getRow(0);
            assertTrue(titleRow.getCell(0).getStringCellValue().contains("CredFlow"));

            var kpiHeaderRow = dashboard.getRow(3);
            assertNotNull(kpiHeaderRow);
            assertEquals("Total Receitas", kpiHeaderRow.getCell(0).getStringCellValue());
            assertEquals("Total Despesas", kpiHeaderRow.getCell(1).getStringCellValue());
        }
    }

    @Test
    void exportToExcel_categorySummaryCalculatesCorrectly() throws IOException {
        var filter = createFilter();
        var transactions = createSampleTransactions();

        when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(transactions);

        var result = service.exportToExcel(filter);

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            var categorySheet = workbook.getSheet("Por Categoria");
            assertNotNull(categorySheet);

            var headerRow = categorySheet.getRow(2);
            assertEquals("Categoria", headerRow.getCell(0).getStringCellValue());
            assertEquals("Receitas", headerRow.getCell(1).getStringCellValue());
            assertEquals("Despesas", headerRow.getCell(2).getStringCellValue());
        }
    }

    @Test
    void exportToExcel_handlesEmptyTransactionList() throws IOException {
        var filter = createFilter();

        when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of());

        var result = service.exportToExcel(filter);

        assertNotNull(result);
        assertTrue(result.length > 0);

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            assertNotNull(workbook.getSheet("Dashboard"));
            assertNotNull(workbook.getSheet("Transações"));
        }
    }

    @Test
    void exportToExcel_handlesTransactionsWithoutCategory() throws IOException {
        var filter = createFilter();
        var transaction = createTransaction(1L, BigDecimal.valueOf(-100), "Test", null, null, null);

        when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(transaction));

        var result = service.exportToExcel(filter);

        assertNotNull(result);

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            var categorySheet = workbook.getSheet("Por Categoria");
            assertNotNull(categorySheet);
        }
    }

    @Test
    void exportToExcel_handlesTransactionsWithoutCreditCard() throws IOException {
        var filter = createFilter();
        var category = createCategory("Alimentação");
        var transaction = createTransaction(1L, BigDecimal.valueOf(-100), "Test", category, null, null);

        when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(transaction));

        var result = service.exportToExcel(filter);

        assertNotNull(result);

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            var cardSheet = workbook.getSheet("Por Cartão");
            assertNotNull(cardSheet);
        }
    }

    @Test
    void exportToExcel_handlesTransactionsWithoutResponsibleUsers() throws IOException {
        var filter = createFilter();
        var category = createCategory("Alimentação");
        var transaction = createTransaction(1L, BigDecimal.valueOf(-100), "Test", category, null, null);

        when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(transaction));

        var result = service.exportToExcel(filter);

        assertNotNull(result);

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            var userSheet = workbook.getSheet("Por Responsável");
            assertNotNull(userSheet);
        }
    }

    @Test
    void exportToExcel_separatesIncomeAndExpenses() throws IOException {
        var filter = createFilter();
        var category = createCategory("Salário");
        var income = createTransaction(1L, BigDecimal.valueOf(5000), "Salário", category, null, null);
        var expense = createTransaction(2L, BigDecimal.valueOf(-200), "Compra", category, null, null);

        when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(income, expense));

        var result = service.exportToExcel(filter);

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result))) {
            var dashboard = workbook.getSheet("Dashboard");
            var kpiValueRow = dashboard.getRow(4);

            var totalIncome = kpiValueRow.getCell(0).getNumericCellValue();
            var totalExpense = kpiValueRow.getCell(1).getNumericCellValue();

            assertEquals(5000.0, totalIncome, 0.01);
            assertEquals(-200.0, totalExpense, 0.01);
        }
    }

    private TransactionFilter createFilter() {
        return new TransactionFilter(
                1L,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 31),
                null, null, null, null, null, null, null, null, null, false
        );
    }

    private List<Transaction> createSampleTransactions() {
        var category1 = createCategory("Alimentação");
        var category2 = createCategory("Transporte");
        var creditCard = createCreditCard("Nubank");
        var user = createUser("João");

        return List.of(
                createTransaction(1L, BigDecimal.valueOf(-150.50), "Supermercado", category1, creditCard, Set.of(user)),
                createTransaction(2L, BigDecimal.valueOf(-35.00), "Uber", category2, null, Set.of(user)),
                createTransaction(3L, BigDecimal.valueOf(3000.00), "Salário", null, null, null),
                createTransaction(4L, BigDecimal.valueOf(-89.90), "Restaurante", category1, creditCard, Set.of(user))
        );
    }

    private Transaction createTransaction(Long id, BigDecimal value, String description,
                                          Category category, CreditCard creditCard, Set<User> users) {
        var account = new Account();
        account.setId(1L);

        var tx = new Transaction();
        tx.setId(id);
        tx.setValue(value);
        tx.setDescription(description);
        tx.setDate(LocalDate.of(2025, 1, 15));
        tx.setCategory(category);
        tx.setCreditCard(creditCard);
        tx.setResponsibleUsers(users);
        tx.setAccount(account);
        tx.setTransactionType(TransactionType.ONE_TIME);
        tx.setSource(TransactionSource.MANUAL);

        return tx;
    }

    private Category createCategory(String name) {
        var category = new Category();
        category.setId(1L);
        category.setName(name);
        return category;
    }

    private CreditCard createCreditCard(String nickname) {
        var card = new CreditCard();
        card.setId(1L);
        card.setNickname(nickname);
        return card;
    }

    private User createUser(String name) {
        var user = new User();
        user.setId(1L);
        user.setName(name);
        return user;
    }
}
