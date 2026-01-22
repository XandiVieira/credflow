package com.relyon.credflow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relyon.credflow.model.transaction.TransactionRequestDTO;
import com.relyon.credflow.model.transaction.TransactionType;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
class DashboardControllerIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;

    @Test
    void getDashboardSummary_withTransactions_shouldReturnCorrectData() throws Exception {
        var ctx = registerAndLogin("dashboard_summary");
        var foodCat = createCategory("Food", ctx.bearer());
        var transportCat = createCategory("Transport", ctx.bearer());

        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 5), "1000.00", "Salary", null);
        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 10), "-200.00", "Groceries", foodCat);
        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 15), "-150.00", "Uber", transportCat);
        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 20), "-100.00", "Restaurant", foodCat);

        var res = mvc.perform(get("/v1/dashboard/summary")
                        .header("Authorization", ctx.bearer())
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalIncome").value(1000.00))
                .andExpect(jsonPath("$.totalExpense").value(450.00))
                .andExpect(jsonPath("$.balance").value(550.00))
                .andExpect(jsonPath("$.topCategories").isArray())
                .andExpect(jsonPath("$.topCategories[0].categoryName").value("Food"))
                .andExpect(jsonPath("$.topCategories[0].totalAmount").value(300.00))
                .andExpect(jsonPath("$.topCategories[0].transactionCount").value(2))
                .andExpect(jsonPath("$.balanceTrend").isArray())
                .andReturn();

        var json = read(res);
        assertThat(json.get("topCategories")).hasSize(2);
    }

    @Test
    void getDashboardSummary_withNoTransactions_shouldReturnZeros() throws Exception {
        var ctx = registerAndLogin("dashboard_empty");

        mvc.perform(get("/v1/dashboard/summary")
                        .header("Authorization", ctx.bearer())
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(0))
                .andExpect(jsonPath("$.totalExpense").value(0))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.topCategories").isEmpty())
                .andExpect(jsonPath("$.upcomingBills").isEmpty());
    }

    @Test
    void getExpenseTrend_withMultipleDates_shouldGroupByDate() throws Exception {
        var ctx = registerAndLogin("expense_trend");
        var category = createCategory("Test", ctx.bearer());

        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 5), "-100.00", "Expense 1", category);
        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 5), "-50.00", "Expense 2", category);
        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 10), "-200.00", "Expense 3", category);

        var res = mvc.perform(get("/v1/dashboard/visualization/expense-trend")
                        .header("Authorization", ctx.bearer())
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataPoints").isArray())
                .andReturn();

        var json = read(res);
        assertThat(json.get("dataPoints")).hasSize(2);

        var dataPoints = json.get("dataPoints");
        var jan5 = dataPoints.get(0);
        assertThat(jan5.get("date").asText()).isEqualTo("2025-01-05");
        assertThat(jan5.get("value").asDouble()).isEqualTo(150.00);

        var jan10 = dataPoints.get(1);
        assertThat(jan10.get("date").asText()).isEqualTo("2025-01-10");
        assertThat(jan10.get("value").asDouble()).isEqualTo(200.00);
    }

    @Test
    void getExpenseTrend_withNoExpenses_shouldReturnEmpty() throws Exception {
        var ctx = registerAndLogin("expense_trend_empty");
        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 5), "1000.00", "Income", null);

        mvc.perform(get("/v1/dashboard/visualization/expense-trend")
                        .header("Authorization", ctx.bearer())
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataPoints").isEmpty());
    }

    @Test
    void getCategoryDistribution_withMultipleCategories_shouldCalculatePercentages() throws Exception {
        var ctx = registerAndLogin("category_dist");
        var foodCat = createCategory("Food", ctx.bearer());
        var transportCat = createCategory("Transport", ctx.bearer());
        var entertainmentCat = createCategory("Entertainment", ctx.bearer());

        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 5), "-400.00", "Groceries", foodCat);
        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 10), "-100.00", "Restaurant", foodCat);
        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 15), "-300.00", "Uber", transportCat);
        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 20), "-100.00", "Taxi", transportCat);
        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 25), "-100.00", "Cinema", entertainmentCat);

        var res = mvc.perform(get("/v1/dashboard/visualization/category-distribution")
                        .header("Authorization", ctx.bearer())
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1000.00))
                .andExpect(jsonPath("$.slices").isArray())
                .andExpect(jsonPath("$.slices[0].categoryName").value("Food"))
                .andExpect(jsonPath("$.slices[0].amount").value(500.00))
                .andExpect(jsonPath("$.slices[0].percentage").value(50.00))
                .andExpect(jsonPath("$.slices[0].color").exists())
                .andReturn();

        var json = read(res);
        assertThat(json.get("slices")).hasSize(3);
    }

    @Test
    void getCategoryDistribution_withNoCategories_shouldReturnEmpty() throws Exception {
        var ctx = registerAndLogin("category_dist_empty");
        createTransaction(ctx.bearer(), LocalDate.of(2025, 1, 5), "-100.00", "Uncategorized", null);

        mvc.perform(get("/v1/dashboard/visualization/category-distribution")
                        .header("Authorization", ctx.bearer())
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.slices").isEmpty());
    }

    @Test
    void getDashboardSummary_missingDates_shouldReturn400() throws Exception {
        var ctx = registerAndLogin("dashboard_missing_dates");

        mvc.perform(get("/v1/dashboard/summary")
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getDashboardSummary_invalidDateFormat_shouldReturn500() throws Exception {
        var ctx = registerAndLogin("dashboard_invalid_date");

        mvc.perform(get("/v1/dashboard/summary")
                        .header("Authorization", ctx.bearer())
                        .param("startDate", "invalid")
                        .param("endDate", "2025-01-31"))
                .andExpect(status().isInternalServerError());
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

    private void createTransaction(String bearer, LocalDate date, String value, String description, Long categoryId) throws Exception {
        var req = new TransactionRequestDTO();
        req.setDate(date);
        req.setDescription(description);
        req.setSimplifiedDescription(description);
        req.setCategoryId(categoryId);
        req.setValue(new BigDecimal(value));
        req.setTransactionType(TransactionType.ONE_TIME);

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
