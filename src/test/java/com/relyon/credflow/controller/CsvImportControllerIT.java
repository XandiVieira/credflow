package com.relyon.credflow.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CsvImportControllerIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;

    @Test
    void importCsv_validFile_shouldImportTransactions() throws Exception {
        var ctx = registerAndLogin("csv_import");

        var csvContent = "date,description,value\n2025-01-15,Test Transaction,100.00\n2025-01-16,Another Transaction,200.00";
        var file = new MockMultipartFile(
                "file",
                "transactions.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        mvc.perform(multipart("/v1/csv/import")
                        .file(file)
                        .header("Authorization", ctx.bearer())
                        .param("format", "GENERIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").exists())
                .andExpect(jsonPath("$.skipped").exists())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void importCsv_emptyFile_shouldReturn400() throws Exception {
        var ctx = registerAndLogin("csv_empty");

        var file = new MockMultipartFile(
                "file",
                "empty.csv",
                "text/csv",
                new byte[0]
        );

        mvc.perform(multipart("/v1/csv/import")
                        .file(file)
                        .header("Authorization", ctx.bearer())
                        .param("format", "GENERIC"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importCsv_invalidFormat_shouldReturn400() throws Exception {
        var ctx = registerAndLogin("csv_invalid_format");

        var csvContent = "invalid,headers,only";
        var file = new MockMultipartFile(
                "file",
                "invalid.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        mvc.perform(multipart("/v1/csv/import")
                        .file(file)
                        .header("Authorization", ctx.bearer())
                        .param("format", "GENERIC"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importCsv_withCreditCard_shouldAssociateTransactions() throws Exception {
        var ctx = registerAndLogin("csv_creditcard");
        var creditCardId = createCreditCard(ctx.bearer());

        var csvContent = "date,description,value\n2025-01-15,CC Purchase,150.00";
        var file = new MockMultipartFile(
                "file",
                "cc_transactions.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        mvc.perform(multipart("/v1/csv/import")
                        .file(file)
                        .header("Authorization", ctx.bearer())
                        .param("format", "GENERIC")
                        .param("creditCardId", String.valueOf(creditCardId)))
                .andExpect(status().isOk());
    }

    @Test
    void importCsv_nubank_shouldParseCorrectly() throws Exception {
        var ctx = registerAndLogin("csv_nubank");

        var csvContent = "date,category,title,amount\n2025-01-15,food,RESTAURANT,50.00";
        var file = new MockMultipartFile(
                "file",
                "nubank.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        mvc.perform(multipart("/v1/csv/import")
                        .file(file)
                        .header("Authorization", ctx.bearer())
                        .param("format", "NUBANK"))
                .andExpect(status().isOk());
    }

    @Test
    void getImportHistory_shouldReturnHistory() throws Exception {
        var ctx = registerAndLogin("csv_history");

        mvc.perform(get("/v1/csv/history")
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getImportFormats_shouldReturnAvailableFormats() throws Exception {
        var ctx = registerAndLogin("csv_formats");

        mvc.perform(get("/v1/csv/formats")
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void endpoints_withoutAuth_shouldReturn401() throws Exception {
        var csvContent = "date,description,value\n2025-01-15,Test,100.00";
        var file = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        mvc.perform(multipart("/v1/csv/import")
                        .file(file)
                        .param("format", "GENERIC"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/v1/csv/history"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/v1/csv/formats"))
                .andExpect(status().isUnauthorized());
    }

    private record Ctx(String token, long accountId, long userId) {
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

        var registerData = read(register);
        var accountId = registerData.get("accountId").asLong();
        var userId = registerData.get("id").asLong();

        var login = mvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Str0ngP@ss!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        var token = read(login).get("token").asText();
        return new Ctx(token, accountId, userId);
    }

    private Long createCreditCard(String bearer) throws Exception {
        var res = mvc.perform(post("/v1/credit-cards")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "nickname", "Import Card",
                                "brand", "MASTERCARD",
                                "lastFourDigits", "5678",
                                "creditLimit", 5000.00,
                                "closingDay", 10,
                                "dueDay", 20
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return read(res).get("id").asLong();
    }

    private JsonNode read(MvcResult mvcResult) throws Exception {
        return om.readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
