package com.relyon.credflow.service;

import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.transaction.TransactionFilter;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.TransactionRepository;
import com.relyon.credflow.specification.TransactionSpecFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdvancedExcelExportService {

    private final TransactionRepository transactionRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM/yyyy");

    @Transactional(readOnly = true)
    public byte[] exportToExcel(TransactionFilter filter) throws IOException {
        log.info("Generating advanced Excel export for account {} from {} to {}",
                filter.accountId(), filter.fromDate(), filter.toDate());

        var spec = TransactionSpecFactory.from(filter);
        var transactions = transactionRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "date"));

        try (var workbook = new XSSFWorkbook()) {
            var styles = createStyles(workbook);

            createDashboardSheet(workbook, transactions, styles, filter);
            createTransactionsSheet(workbook, transactions, styles);
            createCategorySummarySheet(workbook, transactions, styles);
            createMonthlySummarySheet(workbook, transactions, styles);
            createCreditCardSummarySheet(workbook, transactions, styles);
            createUserSummarySheet(workbook, transactions, styles);
            createDailyTrendSheet(workbook, transactions, styles);

            workbook.setActiveSheet(0);

            var baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    private Map<String, CellStyle> createStyles(XSSFWorkbook workbook) {
        var styles = new HashMap<String, CellStyle>();

        var titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 18);
        titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
        var titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        styles.put("title", titleStyle);

        var subtitleFont = workbook.createFont();
        subtitleFont.setBold(true);
        subtitleFont.setFontHeightInPoints((short) 12);
        subtitleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        var subtitleStyle = workbook.createCellStyle();
        subtitleStyle.setFont(subtitleFont);
        styles.put("subtitle", subtitleStyle);

        var headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        var headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        styles.put("header", headerStyle);

        var currencyFormat = workbook.createDataFormat();
        var currencyStyle = workbook.createCellStyle();
        currencyStyle.setDataFormat(currencyFormat.getFormat("R$ #,##0.00;[Red]-R$ #,##0.00"));
        currencyStyle.setBorderBottom(BorderStyle.THIN);
        currencyStyle.setBorderTop(BorderStyle.THIN);
        currencyStyle.setBorderLeft(BorderStyle.THIN);
        currencyStyle.setBorderRight(BorderStyle.THIN);
        styles.put("currency", currencyStyle);

        var percentFormat = workbook.createDataFormat();
        var percentStyle = workbook.createCellStyle();
        percentStyle.setDataFormat(percentFormat.getFormat("0.00%"));
        percentStyle.setBorderBottom(BorderStyle.THIN);
        percentStyle.setBorderTop(BorderStyle.THIN);
        percentStyle.setBorderLeft(BorderStyle.THIN);
        percentStyle.setBorderRight(BorderStyle.THIN);
        styles.put("percent", percentStyle);

        var dateFormat = workbook.createDataFormat();
        var dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(dateFormat.getFormat("dd/mm/yyyy"));
        dateStyle.setBorderBottom(BorderStyle.THIN);
        dateStyle.setBorderTop(BorderStyle.THIN);
        dateStyle.setBorderLeft(BorderStyle.THIN);
        dateStyle.setBorderRight(BorderStyle.THIN);
        styles.put("date", dateStyle);

        var normalStyle = workbook.createCellStyle();
        normalStyle.setBorderBottom(BorderStyle.THIN);
        normalStyle.setBorderTop(BorderStyle.THIN);
        normalStyle.setBorderLeft(BorderStyle.THIN);
        normalStyle.setBorderRight(BorderStyle.THIN);
        normalStyle.setWrapText(true);
        styles.put("normal", normalStyle);

        var incomeFont = workbook.createFont();
        incomeFont.setColor(IndexedColors.DARK_GREEN.getIndex());
        incomeFont.setBold(true);
        var incomeStyle = workbook.createCellStyle();
        incomeStyle.cloneStyleFrom(currencyStyle);
        incomeStyle.setFont(incomeFont);
        styles.put("income", incomeStyle);

        var expenseFont = workbook.createFont();
        expenseFont.setColor(IndexedColors.DARK_RED.getIndex());
        expenseFont.setBold(true);
        var expenseStyle = workbook.createCellStyle();
        expenseStyle.cloneStyleFrom(currencyStyle);
        expenseStyle.setFont(expenseFont);
        styles.put("expense", expenseStyle);

        var kpiTitleFont = workbook.createFont();
        kpiTitleFont.setBold(true);
        kpiTitleFont.setFontHeightInPoints((short) 10);
        var kpiTitleStyle = workbook.createCellStyle();
        kpiTitleStyle.setFont(kpiTitleFont);
        kpiTitleStyle.setAlignment(HorizontalAlignment.CENTER);
        kpiTitleStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        kpiTitleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("kpiTitle", kpiTitleStyle);

        var kpiValueFont = workbook.createFont();
        kpiValueFont.setBold(true);
        kpiValueFont.setFontHeightInPoints((short) 14);
        var kpiValueStyle = workbook.createCellStyle();
        kpiValueStyle.setFont(kpiValueFont);
        kpiValueStyle.setAlignment(HorizontalAlignment.CENTER);
        kpiValueStyle.setDataFormat(currencyFormat.getFormat("R$ #,##0.00"));
        styles.put("kpiValue", kpiValueStyle);

        return styles;
    }

    private void createDashboardSheet(XSSFWorkbook workbook, List<Transaction> transactions,
                                      Map<String, CellStyle> styles, TransactionFilter filter) {
        var sheet = workbook.createSheet("Dashboard");
        sheet.setColumnWidth(0, 5000);
        sheet.setColumnWidth(1, 5000);
        sheet.setColumnWidth(2, 5000);
        sheet.setColumnWidth(3, 5000);
        sheet.setColumnWidth(4, 5000);

        var rowNum = 0;

        var titleRow = sheet.createRow(rowNum++);
        var titleCell = titleRow.createCell(0);
        titleCell.setCellValue("CredFlow - Relatório Financeiro");
        titleCell.setCellStyle(styles.get("title"));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

        var periodRow = sheet.createRow(rowNum++);
        var periodCell = periodRow.createCell(0);
        var periodText = isFullExport(filter)
                ? "Período: Todas as transações"
                : "Período: " + formatDate(filter.fromDate()) + " a " + formatDate(filter.toDate());
        periodCell.setCellValue(periodText);
        periodCell.setCellStyle(styles.get("subtitle"));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 4));

        rowNum++;

        var summary = calculateSummary(transactions);
        var totalIncome = summary[0];
        var totalExpense = summary[1];
        var balance = summary[2];
        var transactionCount = transactions.size();
        var avgTransaction = transactionCount > 0
                ? totalExpense.divide(BigDecimal.valueOf(transactionCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        var kpiHeaderRow = sheet.createRow(rowNum++);
        createKpiHeader(kpiHeaderRow, 0, "Total Receitas", styles);
        createKpiHeader(kpiHeaderRow, 1, "Total Despesas", styles);
        createKpiHeader(kpiHeaderRow, 2, "Saldo", styles);
        createKpiHeader(kpiHeaderRow, 3, "Qtd Transações", styles);
        createKpiHeader(kpiHeaderRow, 4, "Média por Transação", styles);

        var kpiValueRow = sheet.createRow(rowNum++);
        createKpiValue(kpiValueRow, 0, totalIncome, styles, "income");
        createKpiValue(kpiValueRow, 1, totalExpense.negate(), styles, "expense");
        createKpiValue(kpiValueRow, 2, balance, styles, balance.compareTo(BigDecimal.ZERO) >= 0 ? "income" : "expense");
        var countCell = kpiValueRow.createCell(3);
        countCell.setCellValue(transactionCount);
        countCell.setCellStyle(styles.get("kpiValue"));
        createKpiValue(kpiValueRow, 4, avgTransaction.negate(), styles, "expense");

        rowNum += 2;

        var topCategoriesTitle = sheet.createRow(rowNum++);
        topCategoriesTitle.createCell(0).setCellValue("Top 10 Categorias por Gasto");
        topCategoriesTitle.getCell(0).setCellStyle(styles.get("subtitle"));

        var categoryTotals = calculateCategoryTotals(transactions);
        var topCategories = categoryTotals.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(10)
                .toList();

        var catHeaderRow = sheet.createRow(rowNum++);
        createCell(catHeaderRow, 0, "Categoria", styles.get("header"));
        createCell(catHeaderRow, 1, "Total", styles.get("header"));
        createCell(catHeaderRow, 2, "% do Total", styles.get("header"));

        for (var entry : topCategories) {
            var row = sheet.createRow(rowNum++);
            createCell(row, 0, entry.getKey(), styles.get("normal"));
            var valueCell = row.createCell(1);
            valueCell.setCellValue(entry.getValue().negate().doubleValue());
            valueCell.setCellStyle(styles.get("currency"));
            var percentCell = row.createCell(2);
            var percent = totalExpense.compareTo(BigDecimal.ZERO) > 0
                    ? entry.getValue().divide(totalExpense, 4, RoundingMode.HALF_UP).doubleValue()
                    : 0;
            percentCell.setCellValue(percent);
            percentCell.setCellStyle(styles.get("percent"));
        }

        if (!topCategories.isEmpty()) {
            createPieChart(sheet, rowNum - topCategories.size(), rowNum - 1, 4, 20, "Distribuição por Categoria");
        }
    }

    private void createTransactionsSheet(XSSFWorkbook workbook, List<Transaction> transactions,
                                         Map<String, CellStyle> styles) {
        var sheet = workbook.createSheet("Transações");

        var headers = new String[]{"Data", "Descrição", "Descrição Simplificada", "Categoria",
                "Responsáveis", "Cartão de Crédito", "Valor", "Tipo", "Origem", "Parcela"};

        var headerRow = sheet.createRow(0);
        for (var i = 0; i < headers.length; i++) {
            createCell(headerRow, i, headers[i], styles.get("header"));
        }

        var rowNum = 1;
        for (var transaction : transactions) {
            var row = sheet.createRow(rowNum++);

            var dateCell = row.createCell(0);
            dateCell.setCellValue(transaction.getDate());
            dateCell.setCellStyle(styles.get("date"));

            createCell(row, 1, transaction.getDescription(), styles.get("normal"));
            createCell(row, 2, transaction.getSimplifiedDescription(), styles.get("normal"));
            createCell(row, 3, transaction.getCategory() != null ? transaction.getCategory().getName() : "", styles.get("normal"));
            createCell(row, 4, formatResponsibleUsers(transaction), styles.get("normal"));
            createCell(row, 5, transaction.getCreditCard() != null ? transaction.getCreditCard().getNickname() : "", styles.get("normal"));

            var valueCell = row.createCell(6);
            valueCell.setCellValue(transaction.getValue().doubleValue());
            valueCell.setCellStyle(transaction.getValue().compareTo(BigDecimal.ZERO) >= 0
                    ? styles.get("income") : styles.get("expense"));

            createCell(row, 7, transaction.getTransactionType() != null ? transaction.getTransactionType().name() : "", styles.get("normal"));
            createCell(row, 8, transaction.getSource() != null ? transaction.getSource().name() : "", styles.get("normal"));

            var installment = "";
            if (transaction.getCurrentInstallment() != null && transaction.getTotalInstallments() != null) {
                installment = transaction.getCurrentInstallment() + "/" + transaction.getTotalInstallments();
            }
            createCell(row, 9, installment, styles.get("normal"));
        }

        for (var i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        if (!transactions.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(0, rowNum - 1, 0, headers.length - 1));
        }

        sheet.createFreezePane(0, 1);
    }

    private void createCategorySummarySheet(XSSFWorkbook workbook, List<Transaction> transactions,
                                            Map<String, CellStyle> styles) {
        var sheet = workbook.createSheet("Por Categoria");

        var titleRow = sheet.createRow(0);
        titleRow.createCell(0).setCellValue("Resumo por Categoria");
        titleRow.getCell(0).setCellStyle(styles.get("title"));

        var headers = new String[]{"Categoria", "Receitas", "Despesas", "Saldo", "Qtd", "% Despesas"};
        var headerRow = sheet.createRow(2);
        for (var i = 0; i < headers.length; i++) {
            createCell(headerRow, i, headers[i], styles.get("header"));
        }

        var categoryData = calculateCategoryDetails(transactions);
        var totalExpense = transactions.stream()
                .filter(transaction -> transaction.getValue().compareTo(BigDecimal.ZERO) < 0)
                .map(transaction -> transaction.getValue().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var rowNum = 3;
        var dataStartRow = rowNum;
        for (var entry : categoryData.entrySet()) {
            var row = sheet.createRow(rowNum++);
            var data = entry.getValue();

            createCell(row, 0, entry.getKey(), styles.get("normal"));

            var incomeCell = row.createCell(1);
            incomeCell.setCellValue(data.income.doubleValue());
            incomeCell.setCellStyle(styles.get("income"));

            var expenseCell = row.createCell(2);
            expenseCell.setCellValue(data.expense.negate().doubleValue());
            expenseCell.setCellStyle(styles.get("expense"));

            var balanceCell = row.createCell(3);
            balanceCell.setCellValue(data.income.subtract(data.expense).doubleValue());
            balanceCell.setCellStyle(styles.get("currency"));

            var countCell = row.createCell(4);
            countCell.setCellValue(data.count);
            countCell.setCellStyle(styles.get("normal"));

            var percentCell = row.createCell(5);
            var percent = totalExpense.compareTo(BigDecimal.ZERO) > 0
                    ? data.expense.divide(totalExpense, 4, RoundingMode.HALF_UP).doubleValue()
                    : 0;
            percentCell.setCellValue(percent);
            percentCell.setCellStyle(styles.get("percent"));
        }

        for (var i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        if (!categoryData.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(2, rowNum - 1, 0, headers.length - 1));
            createBarChart(sheet, dataStartRow, rowNum - 1, 7, 25, "Despesas por Categoria", 0, 2);
        }
    }

    private void createMonthlySummarySheet(XSSFWorkbook workbook, List<Transaction> transactions,
                                           Map<String, CellStyle> styles) {
        var sheet = workbook.createSheet("Por Mês");

        var titleRow = sheet.createRow(0);
        titleRow.createCell(0).setCellValue("Resumo Mensal");
        titleRow.getCell(0).setCellStyle(styles.get("title"));

        var headers = new String[]{"Mês", "Receitas", "Despesas", "Saldo", "Saldo Acumulado", "Qtd"};
        var headerRow = sheet.createRow(2);
        for (var i = 0; i < headers.length; i++) {
            createCell(headerRow, i, headers[i], styles.get("header"));
        }

        var monthlyData = calculateMonthlyDetails(transactions);
        var rowNum = 3;
        var dataStartRow = rowNum;
        var accumulatedBalance = BigDecimal.ZERO;

        for (var entry : monthlyData.entrySet()) {
            var row = sheet.createRow(rowNum++);
            var data = entry.getValue();

            createCell(row, 0, entry.getKey().format(MONTH_FORMATTER), styles.get("normal"));

            var incomeCell = row.createCell(1);
            incomeCell.setCellValue(data.income.doubleValue());
            incomeCell.setCellStyle(styles.get("income"));

            var expenseCell = row.createCell(2);
            expenseCell.setCellValue(data.expense.negate().doubleValue());
            expenseCell.setCellStyle(styles.get("expense"));

            var balance = data.income.subtract(data.expense);
            var balanceCell = row.createCell(3);
            balanceCell.setCellValue(balance.doubleValue());
            balanceCell.setCellStyle(balance.compareTo(BigDecimal.ZERO) >= 0
                    ? styles.get("income") : styles.get("expense"));

            accumulatedBalance = accumulatedBalance.add(balance);
            var accBalanceCell = row.createCell(4);
            accBalanceCell.setCellValue(accumulatedBalance.doubleValue());
            accBalanceCell.setCellStyle(accumulatedBalance.compareTo(BigDecimal.ZERO) >= 0
                    ? styles.get("income") : styles.get("expense"));

            var countCell = row.createCell(5);
            countCell.setCellValue(data.count);
            countCell.setCellStyle(styles.get("normal"));
        }

        for (var i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        if (!monthlyData.isEmpty()) {
            createLineChart(sheet, dataStartRow, rowNum - 1, 7, 25, "Evolução Mensal", 0, 1, 2);
        }
    }

    private void createCreditCardSummarySheet(XSSFWorkbook workbook, List<Transaction> transactions,
                                              Map<String, CellStyle> styles) {
        var sheet = workbook.createSheet("Por Cartão");

        var titleRow = sheet.createRow(0);
        titleRow.createCell(0).setCellValue("Resumo por Cartão de Crédito");
        titleRow.getCell(0).setCellStyle(styles.get("title"));

        var headers = new String[]{"Cartão", "Total Gasto", "Qtd Transações", "Ticket Médio", "% do Total"};
        var headerRow = sheet.createRow(2);
        for (var i = 0; i < headers.length; i++) {
            createCell(headerRow, i, headers[i], styles.get("header"));
        }

        var cardData = calculateCreditCardDetails(transactions);
        var totalExpense = transactions.stream()
                .filter(transaction -> transaction.getValue().compareTo(BigDecimal.ZERO) < 0)
                .map(transaction -> transaction.getValue().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var rowNum = 3;
        for (var entry : cardData.entrySet()) {
            var row = sheet.createRow(rowNum++);
            var data = entry.getValue();

            createCell(row, 0, entry.getKey(), styles.get("normal"));

            var expenseCell = row.createCell(1);
            expenseCell.setCellValue(data.expense.negate().doubleValue());
            expenseCell.setCellStyle(styles.get("expense"));

            var countCell = row.createCell(2);
            countCell.setCellValue(data.count);
            countCell.setCellStyle(styles.get("normal"));

            var avgCell = row.createCell(3);
            var avg = data.count > 0
                    ? data.expense.divide(BigDecimal.valueOf(data.count), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            avgCell.setCellValue(avg.negate().doubleValue());
            avgCell.setCellStyle(styles.get("currency"));

            var percentCell = row.createCell(4);
            var percent = totalExpense.compareTo(BigDecimal.ZERO) > 0
                    ? data.expense.divide(totalExpense, 4, RoundingMode.HALF_UP).doubleValue()
                    : 0;
            percentCell.setCellValue(percent);
            percentCell.setCellStyle(styles.get("percent"));
        }

        for (var i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        if (!cardData.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(2, rowNum - 1, 0, headers.length - 1));
        }
    }

    private void createUserSummarySheet(XSSFWorkbook workbook, List<Transaction> transactions,
                                        Map<String, CellStyle> styles) {
        var sheet = workbook.createSheet("Por Responsável");

        var titleRow = sheet.createRow(0);
        titleRow.createCell(0).setCellValue("Resumo por Responsável");
        titleRow.getCell(0).setCellStyle(styles.get("title"));

        var headers = new String[]{"Responsável", "Receitas", "Despesas", "Saldo", "Qtd", "% Despesas"};
        var headerRow = sheet.createRow(2);
        for (var i = 0; i < headers.length; i++) {
            createCell(headerRow, i, headers[i], styles.get("header"));
        }

        var userData = calculateUserDetails(transactions);
        var totalExpense = transactions.stream()
                .filter(transaction -> transaction.getValue().compareTo(BigDecimal.ZERO) < 0)
                .map(transaction -> transaction.getValue().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var rowNum = 3;
        for (var entry : userData.entrySet()) {
            var row = sheet.createRow(rowNum++);
            var data = entry.getValue();

            createCell(row, 0, entry.getKey(), styles.get("normal"));

            var incomeCell = row.createCell(1);
            incomeCell.setCellValue(data.income.doubleValue());
            incomeCell.setCellStyle(styles.get("income"));

            var expenseCell = row.createCell(2);
            expenseCell.setCellValue(data.expense.negate().doubleValue());
            expenseCell.setCellStyle(styles.get("expense"));

            var balanceCell = row.createCell(3);
            balanceCell.setCellValue(data.income.subtract(data.expense).doubleValue());
            balanceCell.setCellStyle(styles.get("currency"));

            var countCell = row.createCell(4);
            countCell.setCellValue(data.count);
            countCell.setCellStyle(styles.get("normal"));

            var percentCell = row.createCell(5);
            var percent = totalExpense.compareTo(BigDecimal.ZERO) > 0
                    ? data.expense.divide(totalExpense, 4, RoundingMode.HALF_UP).doubleValue()
                    : 0;
            percentCell.setCellValue(percent);
            percentCell.setCellStyle(styles.get("percent"));
        }

        for (var i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        if (!userData.isEmpty()) {
            sheet.setAutoFilter(new CellRangeAddress(2, rowNum - 1, 0, headers.length - 1));
        }
    }

    private void createDailyTrendSheet(XSSFWorkbook workbook, List<Transaction> transactions,
                                       Map<String, CellStyle> styles) {
        var sheet = workbook.createSheet("Tendência Diária");

        var titleRow = sheet.createRow(0);
        titleRow.createCell(0).setCellValue("Tendência Diária de Gastos");
        titleRow.getCell(0).setCellStyle(styles.get("title"));

        var headers = new String[]{"Data", "Receitas", "Despesas", "Saldo do Dia", "Saldo Acumulado"};
        var headerRow = sheet.createRow(2);
        for (var i = 0; i < headers.length; i++) {
            createCell(headerRow, i, headers[i], styles.get("header"));
        }

        var dailyData = calculateDailyDetails(transactions);
        var rowNum = 3;
        var dataStartRow = rowNum;
        var accumulatedBalance = BigDecimal.ZERO;

        for (var entry : dailyData.entrySet()) {
            var row = sheet.createRow(rowNum++);
            var data = entry.getValue();

            var dateCell = row.createCell(0);
            dateCell.setCellValue(entry.getKey());
            dateCell.setCellStyle(styles.get("date"));

            var incomeCell = row.createCell(1);
            incomeCell.setCellValue(data.income.doubleValue());
            incomeCell.setCellStyle(styles.get("income"));

            var expenseCell = row.createCell(2);
            expenseCell.setCellValue(data.expense.negate().doubleValue());
            expenseCell.setCellStyle(styles.get("expense"));

            var dailyBalance = data.income.subtract(data.expense);
            var balanceCell = row.createCell(3);
            balanceCell.setCellValue(dailyBalance.doubleValue());
            balanceCell.setCellStyle(dailyBalance.compareTo(BigDecimal.ZERO) >= 0
                    ? styles.get("income") : styles.get("expense"));

            accumulatedBalance = accumulatedBalance.add(dailyBalance);
            var accBalanceCell = row.createCell(4);
            accBalanceCell.setCellValue(accumulatedBalance.doubleValue());
            accBalanceCell.setCellStyle(accumulatedBalance.compareTo(BigDecimal.ZERO) >= 0
                    ? styles.get("income") : styles.get("expense"));
        }

        for (var i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        if (dailyData.size() > 1) {
            createLineChart(sheet, dataStartRow, rowNum - 1, 6, 25, "Evolução do Saldo Acumulado", 0, 4, -1);
        }
    }

    private void createPieChart(XSSFSheet sheet, int dataStartRow, int dataEndRow,
                                int chartCol, int chartEndRow, String title) {
        var drawing = sheet.createDrawingPatriarch();
        var anchor = drawing.createAnchor(0, 0, 0, 0, chartCol, 2, chartCol + 6, chartEndRow);

        var chart = drawing.createChart(anchor);
        chart.setTitleText(title);
        chart.setTitleOverlay(false);

        var legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.RIGHT);

        var data = chart.createData(ChartTypes.PIE, null, null);

        var categories = XDDFDataSourcesFactory.fromStringCellRange(sheet,
                new CellRangeAddress(dataStartRow, dataEndRow, 0, 0));
        var values = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
                new CellRangeAddress(dataStartRow, dataEndRow, 1, 1));

        var series = data.addSeries(categories, values);
        series.setTitle(title, null);

        chart.plot(data);
    }

    private void createBarChart(XSSFSheet sheet, int dataStartRow, int dataEndRow,
                                int chartCol, int chartEndRow, String title,
                                int categoryCol, int valueCol) {
        var drawing = sheet.createDrawingPatriarch();
        var anchor = drawing.createAnchor(0, 0, 0, 0, chartCol, 2, chartCol + 8, chartEndRow);

        var chart = drawing.createChart(anchor);
        chart.setTitleText(title);
        chart.setTitleOverlay(false);

        var legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.BOTTOM);

        var bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        var leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setCrosses(AxisCrosses.AUTO_ZERO);

        var data = chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        ((XDDFBarChartData) data).setBarDirection(BarDirection.COL);

        var categories = XDDFDataSourcesFactory.fromStringCellRange(sheet,
                new CellRangeAddress(dataStartRow, dataEndRow, categoryCol, categoryCol));
        var values = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
                new CellRangeAddress(dataStartRow, dataEndRow, valueCol, valueCol));

        var series = data.addSeries(categories, values);
        series.setTitle("Valor", null);

        chart.plot(data);
    }

    private void createLineChart(XSSFSheet sheet, int dataStartRow, int dataEndRow,
                                 int chartCol, int chartEndRow, String title,
                                 int categoryCol, int valueCol1, int valueCol2) {
        var drawing = sheet.createDrawingPatriarch();
        var anchor = drawing.createAnchor(0, 0, 0, 0, chartCol, 2, chartCol + 10, chartEndRow);

        var chart = drawing.createChart(anchor);
        chart.setTitleText(title);
        chart.setTitleOverlay(false);

        var legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.BOTTOM);

        var bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        var leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setCrosses(AxisCrosses.AUTO_ZERO);

        var data = chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);

        var categories = XDDFDataSourcesFactory.fromStringCellRange(sheet,
                new CellRangeAddress(dataStartRow, dataEndRow, categoryCol, categoryCol));

        var values1 = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
                new CellRangeAddress(dataStartRow, dataEndRow, valueCol1, valueCol1));
        var series1 = data.addSeries(categories, values1);
        series1.setTitle(sheet.getRow(2).getCell(valueCol1).getStringCellValue(), null);
        ((XDDFLineChartData.Series) series1).setSmooth(true);
        ((XDDFLineChartData.Series) series1).setMarkerStyle(MarkerStyle.CIRCLE);

        if (valueCol2 > 0) {
            var values2 = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
                    new CellRangeAddress(dataStartRow, dataEndRow, valueCol2, valueCol2));
            var series2 = data.addSeries(categories, values2);
            series2.setTitle(sheet.getRow(2).getCell(valueCol2).getStringCellValue(), null);
            ((XDDFLineChartData.Series) series2).setSmooth(true);
            ((XDDFLineChartData.Series) series2).setMarkerStyle(MarkerStyle.SQUARE);
        }

        chart.plot(data);
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        var cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void createKpiHeader(Row row, int column, String value, Map<String, CellStyle> styles) {
        var cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(styles.get("kpiTitle"));
    }

    private void createKpiValue(Row row, int column, BigDecimal value, Map<String, CellStyle> styles, String styleKey) {
        var cell = row.createCell(column);
        cell.setCellValue(value.doubleValue());
        var style = styles.get("kpiValue");
        if ("income".equals(styleKey)) {
            var workbook = (XSSFWorkbook) row.getSheet().getWorkbook();
            var incomeStyle = workbook.createCellStyle();
            incomeStyle.cloneStyleFrom(style);
            var font = workbook.createFont();
            font.setColor(IndexedColors.DARK_GREEN.getIndex());
            font.setBold(true);
            font.setFontHeightInPoints((short) 14);
            incomeStyle.setFont(font);
            cell.setCellStyle(incomeStyle);
        } else if ("expense".equals(styleKey)) {
            var workbook = (XSSFWorkbook) row.getSheet().getWorkbook();
            var expenseStyle = workbook.createCellStyle();
            expenseStyle.cloneStyleFrom(style);
            var font = workbook.createFont();
            font.setColor(IndexedColors.DARK_RED.getIndex());
            font.setBold(true);
            font.setFontHeightInPoints((short) 14);
            expenseStyle.setFont(font);
            cell.setCellStyle(expenseStyle);
        } else {
            cell.setCellStyle(style);
        }
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    private boolean isFullExport(TransactionFilter filter) {
        return filter.fromDate() != null
                && filter.fromDate().getYear() <= 1900;
    }

    private String formatResponsibleUsers(Transaction transaction) {
        if (transaction.getResponsibleUsers() == null || transaction.getResponsibleUsers().isEmpty()) {
            return "";
        }
        return transaction.getResponsibleUsers().stream()
                .map(User::getName)
                .collect(Collectors.joining(", "));
    }

    private BigDecimal[] calculateSummary(List<Transaction> transactions) {
        var income = transactions.stream()
                .map(Transaction::getValue)
                .filter(value -> value.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var expense = transactions.stream()
                .filter(transaction -> transaction.getValue().compareTo(BigDecimal.ZERO) < 0)
                .map(transaction -> transaction.getValue().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var balance = income.subtract(expense);

        return new BigDecimal[]{income, expense, balance};
    }

    private Map<String, BigDecimal> calculateCategoryTotals(List<Transaction> transactions) {
        return transactions.stream()
                .filter(transaction -> transaction.getValue().compareTo(BigDecimal.ZERO) < 0)
                .collect(Collectors.groupingBy(
                        transaction -> transaction.getCategory() != null ? transaction.getCategory().getName() : "Sem Categoria",
                        Collectors.reducing(BigDecimal.ZERO, transaction -> transaction.getValue().abs(), BigDecimal::add)
                ));
    }

    private Map<String, SummaryData> calculateCategoryDetails(List<Transaction> transactions) {
        var result = new LinkedHashMap<String, SummaryData>();

        var grouped = transactions.stream()
                .collect(Collectors.groupingBy(
                        transaction -> transaction.getCategory() != null ? transaction.getCategory().getName() : "Sem Categoria"
                ));

        grouped.entrySet().stream()
                .sorted((entryA, entryB) -> {
                    var expenseA = entryA.getValue().stream()
                            .filter(transaction -> transaction.getValue().compareTo(BigDecimal.ZERO) < 0)
                            .map(transaction -> transaction.getValue().abs())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    var expenseB = entryB.getValue().stream()
                            .filter(transaction -> transaction.getValue().compareTo(BigDecimal.ZERO) < 0)
                            .map(transaction -> transaction.getValue().abs())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return expenseB.compareTo(expenseA);
                })
                .forEach(entry -> {
                    var data = new SummaryData();
                    for (var transaction : entry.getValue()) {
                        if (transaction.getValue().compareTo(BigDecimal.ZERO) > 0) {
                            data.income = data.income.add(transaction.getValue());
                        } else {
                            data.expense = data.expense.add(transaction.getValue().abs());
                        }
                        data.count++;
                    }
                    result.put(entry.getKey(), data);
                });

        return result;
    }

    private Map<YearMonth, SummaryData> calculateMonthlyDetails(List<Transaction> transactions) {
        var result = new LinkedHashMap<YearMonth, SummaryData>();

        var grouped = transactions.stream()
                .collect(Collectors.groupingBy(
                        transaction -> YearMonth.from(transaction.getDate())
                ));

        grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    var data = new SummaryData();
                    for (var transaction : entry.getValue()) {
                        if (transaction.getValue().compareTo(BigDecimal.ZERO) > 0) {
                            data.income = data.income.add(transaction.getValue());
                        } else {
                            data.expense = data.expense.add(transaction.getValue().abs());
                        }
                        data.count++;
                    }
                    result.put(entry.getKey(), data);
                });

        return result;
    }

    private Map<String, SummaryData> calculateCreditCardDetails(List<Transaction> transactions) {
        var result = new LinkedHashMap<String, SummaryData>();

        var grouped = transactions.stream()
                .filter(transaction -> transaction.getValue().compareTo(BigDecimal.ZERO) < 0)
                .collect(Collectors.groupingBy(
                        transaction -> transaction.getCreditCard() != null ? transaction.getCreditCard().getNickname() : "Sem Cartão"
                ));

        grouped.entrySet().stream()
                .sorted((entryA, entryB) -> {
                    var expenseA = entryA.getValue().stream()
                            .map(transaction -> transaction.getValue().abs())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    var expenseB = entryB.getValue().stream()
                            .map(transaction -> transaction.getValue().abs())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return expenseB.compareTo(expenseA);
                })
                .forEach(entry -> {
                    var data = new SummaryData();
                    for (var transaction : entry.getValue()) {
                        data.expense = data.expense.add(transaction.getValue().abs());
                        data.count++;
                    }
                    result.put(entry.getKey(), data);
                });

        return result;
    }

    private Map<String, SummaryData> calculateUserDetails(List<Transaction> transactions) {
        var result = new LinkedHashMap<String, SummaryData>();

        var userTotals = new HashMap<String, SummaryData>();

        for (var transaction : transactions) {
            var users = transaction.getResponsibleUsers();
            if (users == null || users.isEmpty()) {
                userTotals.computeIfAbsent("Sem Responsável", key -> new SummaryData()).addTransaction(transaction);
            } else {
                for (var user : users) {
                    userTotals.computeIfAbsent(user.getName(), key -> new SummaryData()).addTransaction(transaction);
                }
            }
        }

        userTotals.entrySet().stream()
                .sorted((a, b) -> b.getValue().expense.compareTo(a.getValue().expense))
                .forEach(entry -> result.put(entry.getKey(), entry.getValue()));

        return result;
    }

    private Map<LocalDate, SummaryData> calculateDailyDetails(List<Transaction> transactions) {
        var result = new LinkedHashMap<LocalDate, SummaryData>();

        var grouped = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getDate));

        grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    var data = new SummaryData();
                    for (var transaction : entry.getValue()) {
                        if (transaction.getValue().compareTo(BigDecimal.ZERO) > 0) {
                            data.income = data.income.add(transaction.getValue());
                        } else {
                            data.expense = data.expense.add(transaction.getValue().abs());
                        }
                        data.count++;
                    }
                    result.put(entry.getKey(), data);
                });

        return result;
    }

    private static class SummaryData {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        int count = 0;

        void addTransaction(Transaction transaction) {
            if (transaction.getValue().compareTo(BigDecimal.ZERO) > 0) {
                income = income.add(transaction.getValue());
            } else {
                expense = expense.add(transaction.getValue().abs());
            }
            count++;
        }
    }
}
