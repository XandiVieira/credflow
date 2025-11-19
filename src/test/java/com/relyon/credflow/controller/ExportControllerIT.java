package com.relyon.credflow.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relyon.credflow.model.transaction.TransactionRequestDTO;
import com.relyon.credflow.model.transaction.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ExportControllerIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;

    @Test
    void exportCsv_withTransactions_shouldReturnCsvFile() throws Exception {
        var ctx = registerAndLogin("export_csv");
        var category = createCategory("Food", ctx.bearer());

        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 10), "-100.00", category);
        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 15), "500.00", category);

        var result = mvc.perform(get("/v1/export/csv")
                        .header("Authorization", ctx.bearer())
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"transactions_20250101_20250131.csv\""))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn();

        var csv = result.getResponse().getContentAsString();
        assertThat(csv).contains("Date,Description,Category,Responsible Users,Credit Card,Value,Type");
        assertThat(csv).contains("Food");
        assertThat(csv).contains("-100.00");
        assertThat(csv).contains("500.00");
    }

    @Test
    void exportPdf_withTransactions_shouldReturnPdfFile() throws Exception {
        var ctx = registerAndLogin("export_pdf");
        var category = createCategory("Transport", ctx.bearer());

        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 10), "-200.00", category);

        var result = mvc.perform(get("/v1/export/pdf")
                        .header("Authorization", ctx.bearer())
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"transactions_20250101_20250131.pdf\""))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andReturn();

        assertThat(result.getResponse().getContentAsByteArray()).isNotEmpty();
    }

    @Test
    void exportExcel_withTransactions_shouldReturnExcelFile() throws Exception {
        var ctx = registerAndLogin("export_excel");
        var category = createCategory("Entertainment", ctx.bearer());

        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 10), "-150.00", category);
        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 15), "-50.00", category);

        var result = mvc.perform(get("/v1/export/excel")
                        .header("Authorization", ctx.bearer())
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"transactions_20250101_20250131.xlsx\""))
                .andExpect(content().contentTypeCompatibleWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andReturn();

        assertThat(result.getResponse().getContentAsByteArray()).isNotEmpty();
    }

    @Test
    void exportCsv_withNoTransactions_shouldReturnEmptyCsv() throws Exception {
        var ctx = registerAndLogin("export_csv_empty");

        var result = mvc.perform(get("/v1/export/csv")
                        .header("Authorization", ctx.bearer())
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andReturn();

        var csv = result.getResponse().getContentAsString();
        assertThat(csv).contains("Date,Description,Category,Responsible Users,Credit Card,Value,Type");
        assertThat(csv.split("\n")).hasSize(1);
    }

    @Test
    void exportCsv_missingDates_shouldReturn400() throws Exception {
        var ctx = registerAndLogin("export_missing_dates");

        mvc.perform(get("/v1/export/csv")
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isBadRequest());
    }

    private record Ctx(String token, long accountId) {
        String bearer() {
            return "Bearer " + token;
        }
    }

    private Ctx registerAndLogin(String emailPrefix) throws Exception {
        var email = emailPrefix + "+" + System.nanoTime() + "@example.com";

        var register = mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "name", emailPrefix.toUpperCase(),
                                "email", email,
                                "password", "Str0ngP@ss!",
                                "confirmPassword", "Str0ngP@ss!"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();

        var accountId = read(register).get("accountId").asLong();

        var login = mvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Str0ngP@ss!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        var token = read(login).get("token").asText();
        return new Ctx(token, accountId);
    }

    private Long createCategory(String name, String bearer) throws Exception {
        var res = mvc.perform(post("/v1/categories")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isOk())
                .andReturn();
        return read(res).get("id").asLong();
    }

    private void createTransaction(String bearer, LocalDate date, String value, Long categoryId) throws Exception {
        var req = new TransactionRequestDTO();
        req.setDate(date);
        req.setDescription("Test Transaction");
        req.setSimplifiedDescription("Test");
        req.setValue(new BigDecimal(value));
        req.setTransactionType(TransactionType.ONE_TIME);
        req.setCategoryId(categoryId);

        mvc.perform(post("/v1/transactions")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    private JsonNode read(MvcResult mvcResult) throws Exception {
        return om.readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
