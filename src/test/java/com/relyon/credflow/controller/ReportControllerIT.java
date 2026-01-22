package com.relyon.credflow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relyon.credflow.model.transaction.TransactionRequestDTO;
import com.relyon.credflow.model.transaction.TransactionType;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReportControllerIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;

    @Test
    void getCategoryReport_withTransactions_shouldReturnCorrectData() throws Exception {
        var ctx = registerAndLogin("category_report");
        var foodCat = createCategory("Food", ctx.bearer());
        var transportCat = createCategory("Transport", ctx.bearer());

        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 10), "-400.00", null, foodCat, null);
        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 15), "-100.00", null, transportCat, null);

        var res = mvc.perform(get("/v1/reports/category")
                        .header("Authorization", ctx.bearer())
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(500.00))
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.categories[0].categoryName").value("Food"))
                .andExpect(jsonPath("$.categories[0].amount").value(400.00))
                .andExpect(jsonPath("$.categories[0].percentage").value(80.00))
                .andReturn();

        var json = read(res);
        assertThat(json.get("categories")).hasSize(2);
    }

    @Test
    void getUserReport_withTransactions_shouldReturnCorrectData() throws Exception {
        var ctx = registerAndLogin("user_report");
        var category = createCategory("Test", ctx.bearer());

        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 10), "-300.00", ctx.userId(), category, null);
        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 15), "-200.00", ctx.userId(), category, null);

        var result = mvc.perform(get("/v1/reports/user")
                        .header("Authorization", ctx.bearer())
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andReturn();

        var json = read(result);
        System.out.println("User Report Response: " + json.toPrettyString());
        assertThat(json.get("totalExpense").asDouble()).isEqualTo(500.00);
        assertThat(json.get("users")).isNotNull();
        assertThat(json.get("users")).hasSize(1);
    }

    @Test
    void getCreditCardReport_withTransactions_shouldReturnCorrectData() throws Exception {
        var ctx = registerAndLogin("cc_report");
        var category = createCategory("Test", ctx.bearer());
        var cardId = createCreditCard("Visa", ctx.bearer(), ctx.userId());

        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 10), "-500.00", null, category, cardId);
        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 15), "-300.00", null, category, cardId);

        mvc.perform(get("/v1/reports/credit-card")
                        .header("Authorization", ctx.bearer())
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense").value(800.00))
                .andExpect(jsonPath("$.creditCards").isArray())
                .andExpect(jsonPath("$.creditCards[0].creditCardNickname").value("Visa"))
                .andExpect(jsonPath("$.creditCards[0].amount").value(800.00))
                .andExpect(jsonPath("$.creditCards[0].percentage").value(100.00));
    }

    @Test
    void getMonthComparison_withMultipleMonths_shouldReturnCorrectData() throws Exception {
        var ctx = registerAndLogin("month_comparison");
        var category = createCategory("Test", ctx.bearer());

        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 10), "1000.00", null, category, null);
        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 15), "-400.00", null, category, null);
        createTransaction(ctx.bearer(), LocalDate.of(2025, 2, 10), "1200.00", null, category, null);
        createTransaction(ctx.bearer(), LocalDate.of(2025, 2, 15), "-600.00", null, category, null);

        var res = mvc.perform(get("/v1/reports/month-comparison")
                        .header("Authorization", ctx.bearer())
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-02-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.months").isArray())
                .andExpect(jsonPath("$.months[0].month").value(1))
                .andExpect(jsonPath("$.months[0].income").value(1000.00))
                .andExpect(jsonPath("$.months[0].expense").value(400.00))
                .andExpect(jsonPath("$.summary.totalIncome").value(2200.00))
                .andExpect(jsonPath("$.summary.totalExpense").value(1000.00))
                .andReturn();

        var json = read(res);
        assertThat(json.get("months")).hasSize(2);
    }

    @Test
    void getReports_missingDates_shouldReturn400() throws Exception {
        var ctx = registerAndLogin("report_missing_dates");

        mvc.perform(get("/v1/reports/category")
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isBadRequest());
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

    private Long createCategory(String name, String bearer) throws Exception {
        var res = mvc.perform(post("/v1/categories")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isOk())
                .andReturn();
        return read(res).get("id").asLong();
    }

    private Long createCreditCard(String nickname, String bearer, Long holderId) throws Exception {
        var res = mvc.perform(post("/v1/credit-cards")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "nickname", nickname,
                                "brand", "Visa",
                                "tier", "Gold",
                                "issuer", "Test Bank",
                                "lastFourDigits", "1234",
                                "closingDay", 15,
                                "dueDay", 25,
                                "creditLimit", 10000.00,
                                "holderId", holderId
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return read(res).get("id").asLong();
    }

    private void createTransaction(String bearer, LocalDate date, String value,
                                   Long responsibleId, Long categoryId, Long creditCardId) throws Exception {
        var req = new TransactionRequestDTO();
        req.setDate(date);
        req.setDescription("Test Transaction");
        req.setSimplifiedDescription("Test");
        req.setValue(new BigDecimal(value));
        req.setTransactionType(TransactionType.ONE_TIME);
        if (responsibleId != null) {
            req.setResponsibleUsers(List.of(responsibleId));
        }
        req.setCategoryId(categoryId);
        req.setCreditCardId(creditCardId);

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
