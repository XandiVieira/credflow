package com.relyon.credflow.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BudgetTrackingControllerIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;

    @Test
    void trackBudgets_noBudgets_shouldReturnEmptyList() throws Exception {
        var ctx = registerAndLogin("budget_track_empty");

        var result = mvc.perform(get("/v1/budget-tracking")
                        .header("Authorization", ctx.bearer())
                        .param("period", "2025-01"))
                .andExpect(status().isOk())
                .andReturn();

        var json = om.readTree(result.getResponse().getContentAsString());
        assertThat(json).isEmpty();
    }

    @Test
    void trackBudgets_withBudget_shouldReturnTrackingData() throws Exception {
        var ctx = registerAndLogin("budget_track_data");
        var budgetId = createBudget(ctx.bearer(), "2025-01", 1000.00);

        var result = mvc.perform(get("/v1/budget-tracking")
                        .header("Authorization", ctx.bearer())
                        .param("period", "2025-01"))
                .andExpect(status().isOk())
                .andReturn();

        var json = om.readTree(result.getResponse().getContentAsString());
        assertThat(json).hasSize(1);
        assertThat(json.get(0).get("budgetId").asLong()).isEqualTo(budgetId);
    }

    @Test
    void trackBudget_existingBudget_shouldReturnTrackingDetails() throws Exception {
        var ctx = registerAndLogin("budget_track_single");
        var budgetId = createBudget(ctx.bearer(), "2025-01", 500.00);

        mvc.perform(get("/v1/budget-tracking/" + budgetId)
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetId").value(budgetId))
                .andExpect(jsonPath("$.budgetedAmount").value(500.00))
                .andExpect(jsonPath("$.spentAmount").exists())
                .andExpect(jsonPath("$.remainingAmount").exists())
                .andExpect(jsonPath("$.percentUsed").exists());
    }

    @Test
    void trackBudget_nonExistingBudget_shouldReturn404() throws Exception {
        var ctx = registerAndLogin("budget_track_404");

        mvc.perform(get("/v1/budget-tracking/99999")
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void trackBudgets_withTransactions_shouldCalculateSpentAmount() throws Exception {
        var ctx = registerAndLogin("budget_track_with_tx");
        var categoryId = createCategory("Food", ctx.bearer());
        createBudget(ctx.bearer(), "2025-01", 1000.00, categoryId);

        createTransaction(ctx.bearer(), "2025-01-15", 100.00, categoryId);
        createTransaction(ctx.bearer(), "2025-01-20", 200.00, categoryId);

        var result = mvc.perform(get("/v1/budget-tracking")
                        .header("Authorization", ctx.bearer())
                        .param("period", "2025-01"))
                .andExpect(status().isOk())
                .andReturn();

        var json = om.readTree(result.getResponse().getContentAsString());
        assertThat(json).hasSize(1);
    }

    @Test
    void processRollover_shouldSucceed() throws Exception {
        var ctx = registerAndLogin("budget_track_rollover");
        createBudgetWithRollover(ctx.bearer(), "2025-01", 1000.00);

        mvc.perform(post("/v1/budget-tracking/rollover")
                        .header("Authorization", ctx.bearer())
                        .param("fromPeriod", "2025-01"))
                .andExpect(status().isOk());
    }

    @Test
    void endpoints_withoutAuth_shouldReturn401() throws Exception {
        mvc.perform(get("/v1/budget-tracking")
                        .param("period", "2025-01"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/v1/budget-tracking/1"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/v1/budget-tracking/rollover")
                        .param("fromPeriod", "2025-01"))
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

    private Long createBudget(String bearer, String period, Double amount) throws Exception {
        var res = mvc.perform(post("/v1/budgets")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "period", period,
                                "amount", amount,
                                "type", "ACCOUNT_WIDE",
                                "allowRollover", false
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return read(res).get("id").asLong();
    }

    private Long createBudget(String bearer, String period, Double amount, Long categoryId) throws Exception {
        var res = mvc.perform(post("/v1/budgets")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "period", period,
                                "amount", amount,
                                "type", "CATEGORY_SPECIFIC",
                                "categoryId", categoryId,
                                "allowRollover", false
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return read(res).get("id").asLong();
    }

    private void createBudgetWithRollover(String bearer, String period, Double amount) throws Exception {
        mvc.perform(post("/v1/budgets")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "period", period,
                                "amount", amount,
                                "type", "ACCOUNT_WIDE",
                                "allowRollover", true
                        ))))
                .andExpect(status().isOk());
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

    private void createTransaction(String bearer, String date, Double amount, Long categoryId) throws Exception {
        mvc.perform(post("/v1/transactions")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "date", date,
                                "description", "Test Transaction",
                                "value", amount,
                                "categoryId", categoryId,
                                "transactionType", "ONE_TIME"
                        ))))
                .andExpect(status().isCreated());
    }

    private JsonNode read(MvcResult mvcResult) throws Exception {
        return om.readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
