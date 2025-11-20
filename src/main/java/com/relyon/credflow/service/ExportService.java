package com.relyon.credflow.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.transaction.TransactionFilter;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.TransactionRepository;
import com.relyon.credflow.specification.TransactionSpecFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private final TransactionRepository transactionRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Transactional(readOnly = true)
    public byte[] exportToCsv(TransactionFilter filter) {
        log.info("Exporting transactions to CSV for account {} from {} to {}",
                filter.accountId(), filter.fromDate(), filter.toDate());

        var spec = TransactionSpecFactory.from(filter);
        var transactions = transactionRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "date"));

        var csv = new StringBuilder();
        csv.append("Date,Description,Category,Responsible Users,Credit Card,Value,Type\n");

        for (var transaction : transactions) {
            csv.append(formatDate(transaction.getDate())).append(",");
            csv.append(escapeCsv(transaction.getDescription())).append(",");
            csv.append(escapeCsv(transaction.getCategory() != null ? transaction.getCategory().getName() : "")).append(",");
            csv.append(escapeCsv(formatResponsibleUsers(transaction))).append(",");
            csv.append(escapeCsv(transaction.getCreditCard() != null ? transaction.getCreditCard().getNickname() : "")).append(",");
            csv.append(formatCurrency(transaction.getValue())).append(",");
            csv.append(transaction.getTransactionType() != null ? transaction.getTransactionType().name() : "").append("\n");
        }

        return csv.toString().getBytes();
    }

    @Transactional(readOnly = true)
    public byte[] exportToPdf(TransactionFilter filter) throws IOException {
        log.info("Exporting transactions to PDF for account {} from {} to {}",
                filter.accountId(), filter.fromDate(), filter.toDate());

        var spec = TransactionSpecFactory.from(filter);
        var transactions = transactionRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "date"));

        var baos = new ByteArrayOutputStream();
        var writer = new PdfWriter(baos);
        var pdf = new PdfDocument(writer);
        var document = new Document(pdf);

        document.add(new Paragraph("Transaction Report")
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("Period: " + formatDate(filter.fromDate()) + " to " + formatDate(filter.toDate()))
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("\n"));

        var table = new Table(new float[]{2, 4, 2, 2, 2, 2});
        table.setWidth(550);

        table.addHeaderCell("Date");
        table.addHeaderCell("Description");
        table.addHeaderCell("Category");
        table.addHeaderCell("Responsible Users");
        table.addHeaderCell("Credit Card");
        table.addHeaderCell("Value");

        for (var transaction : transactions) {
            table.addCell(formatDate(transaction.getDate()));
            table.addCell(transaction.getDescription());
            table.addCell(transaction.getCategory() != null ? transaction.getCategory().getName() : "");
            table.addCell(formatResponsibleUsers(transaction));
            table.addCell(transaction.getCreditCard() != null ? transaction.getCreditCard().getNickname() : "");
            table.addCell(formatCurrency(transaction.getValue()));
        }

        document.add(table);

        var summary = calculateSummary(transactions);
        document.add(new Paragraph("\n"));
        document.add(new Paragraph("Summary").setBold());
        document.add(new Paragraph("Total Income: " + formatCurrency(summary[0])));
        document.add(new Paragraph("Total Expense: " + formatCurrency(summary[1])));
        document.add(new Paragraph("Balance: " + formatCurrency(summary[2])));

        document.close();
        return baos.toByteArray();
    }

    @Transactional(readOnly = true)
    public byte[] exportToExcel(TransactionFilter filter) throws IOException {
        log.info("Exporting transactions to Excel for account {} from {} to {}",
                filter.accountId(), filter.fromDate(), filter.toDate());

        var spec = TransactionSpecFactory.from(filter);
        var transactions = transactionRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "date"));

        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("Transactions");

        var headerStyle = workbook.createCellStyle();
        var headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        var headerRow = sheet.createRow(0);
        var headers = new String[]{"Date", "Description", "Category", "Responsible Users", "Credit Card", "Value", "Type"};
        for (var headerIndex = 0; headerIndex < headers.length; headerIndex++) {
            var cell = headerRow.createCell(headerIndex);
            cell.setCellValue(headers[headerIndex]);
            cell.setCellStyle(headerStyle);
        }

        var rowNum = 1;
        for (var transaction : transactions) {
            var row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(formatDate(transaction.getDate()));
            row.createCell(1).setCellValue(transaction.getDescription());
            row.createCell(2).setCellValue(transaction.getCategory() != null ? transaction.getCategory().getName() : "");
            row.createCell(3).setCellValue(formatResponsibleUsers(transaction));
            row.createCell(4).setCellValue(transaction.getCreditCard() != null ? transaction.getCreditCard().getNickname() : "");
            row.createCell(5).setCellValue(transaction.getValue().doubleValue());
            row.createCell(6).setCellValue(transaction.getTransactionType() != null ? transaction.getTransactionType().name() : "");
        }

        for (var columnIndex = 0; columnIndex < headers.length; columnIndex++) {
            sheet.autoSizeColumn(columnIndex);
        }

        var summary = calculateSummary(transactions);
        rowNum += 2;
        var incomeRow = sheet.createRow(rowNum++);
        incomeRow.createCell(0).setCellValue("Total Income:");
        incomeRow.createCell(1).setCellValue(summary[0].doubleValue());

        var expenseRow = sheet.createRow(rowNum++);
        expenseRow.createCell(0).setCellValue("Total Expense:");
        expenseRow.createCell(1).setCellValue(summary[1].doubleValue());

        var balanceRow = sheet.createRow(rowNum);
        balanceRow.createCell(0).setCellValue("Balance:");
        balanceRow.createCell(1).setCellValue(summary[2].doubleValue());

        var baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();

        return baos.toByteArray();
    }

    private String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    private String formatCurrency(BigDecimal value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String formatResponsibleUsers(Transaction transaction) {
        if (transaction.getResponsibleUsers() == null || transaction.getResponsibleUsers().isEmpty()) {
            return "";
        }
        return transaction.getResponsibleUsers().stream()
                .map(User::getName)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private BigDecimal[] calculateSummary(List<Transaction> transactions) {
        var income = transactions.stream()
                .map(Transaction::getValue)
                .filter(value -> value.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var expense = transactions.stream()
                .filter(t -> t.getValue().compareTo(BigDecimal.ZERO) < 0)
                .map(t -> t.getValue().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var balance = income.subtract(expense);

        return new BigDecimal[]{income, expense, balance};
    }
}
