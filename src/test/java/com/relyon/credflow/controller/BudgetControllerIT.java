package com.relyon.credflow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relyon.credflow.model.budget.BudgetType;
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
class BudgetControllerIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;

    @Test
    void createBudget_accountWide_shouldSucceed() throws Exception {
        var ctx = registerAndLogin("budget_account_wide");

        var result = mvc.perform(post("/v1/budgets")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "period", "2025-01",
                                "amount", 1000.00,
                                "type", "ACCOUNT_WIDE",
                                "allowRollover", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.amount").value(1000.00))
                .andExpect(jsonPath("$.type").value("ACCOUNT_WIDE"))
                .andExpect(jsonPath("$.allowRollover").value(true))
                .andExpect(jsonPath("$.effectiveBudget").value(1000.00))
                .andReturn();

        var json = read(result);
        assertThat(json.get("accountId").asLong()).isEqualTo(ctx.accountId());
    }

    @Test
    void createBudget_categorySpecific_shouldSucceed() throws Exception {
        var ctx = registerAndLogin("budget_category");
        var categoryId = createCategory("Food", ctx.bearer());

        mvc.perform(post("/v1/budgets")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "period", "2025-01",
                                "amount", 500.00,
                                "type", "CATEGORY_SPECIFIC",
                                "categoryId", categoryId
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.categoryName").value("Food"));
    }

    @Test
    void createBudget_userSpecific_shouldSucceed() throws Exception {
        var ctx = registerAndLogin("budget_user");

        mvc.perform(post("/v1/budgets")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "period", "2025-01",
                                "amount", 300.00,
                                "type", "USER_SPECIFIC",
                                "userId", ctx.userId()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(ctx.userId()))
                .andExpect(jsonPath("$.userName").exists());
    }

    @Test
    void createBudget_duplicate_shouldReturn409() throws Exception {
        var ctx = registerAndLogin("budget_duplicate");

        var budgetData = om.writeValueAsString(Map.of(
                "period", "2025-01",
                "amount", 1000.00,
                "type", "ACCOUNT_WIDE",
                "allowRollover", false
        ));

        mvc.perform(post("/v1/budgets")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(budgetData))
                .andExpect(status().isOk());

        mvc.perform(post("/v1/budgets")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(budgetData))
                .andExpect(status().isConflict());
    }

    @Test
    void updateBudget_shouldSucceed() throws Exception {
        var ctx = registerAndLogin("budget_update");
        var budgetId = createBudget(ctx.bearer(), "2025-01", 1000.00, BudgetType.ACCOUNT_WIDE, null, null);

        mvc.perform(put("/v1/budgets/" + budgetId)
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "period", "2025-01",
                                "amount", 1500.00,
                                "type", "ACCOUNT_WIDE",
                                "allowRollover", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(1500.00))
                .andExpect(jsonPath("$.allowRollover").value(true));
    }

    @Test
    void findById_existingBudget_shouldReturnBudget() throws Exception {
        var ctx = registerAndLogin("budget_find");
        var budgetId = createBudget(ctx.bearer(), "2025-01", 1000.00, BudgetType.ACCOUNT_WIDE, null, null);

        mvc.perform(get("/v1/budgets/" + budgetId)
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(budgetId))
                .andExpect(jsonPath("$.amount").value(1000.00));
    }

    @Test
    void findByPeriod_shouldReturnAllBudgets() throws Exception {
        var ctx = registerAndLogin("budget_period");
        var categoryId = createCategory("Food", ctx.bearer());

        createBudget(ctx.bearer(), "2025-01", 1000.00, BudgetType.ACCOUNT_WIDE, null, null);
        createBudget(ctx.bearer(), "2025-01", 500.00, BudgetType.CATEGORY_SPECIFIC, categoryId, null);

        var result = mvc.perform(get("/v1/budgets")
                        .header("Authorization", ctx.bearer())
                        .param("period", "2025-01"))
                .andExpect(status().isOk())
                .andReturn();

        var json = om.readTree(result.getResponse().getContentAsString());
        assertThat(json).hasSize(2);
    }

    @Test
    void deleteBudget_shouldSucceed() throws Exception {
        var ctx = registerAndLogin("budget_delete");
        var budgetId = createBudget(ctx.bearer(), "2025-01", 1000.00, BudgetType.ACCOUNT_WIDE, null, null);

        mvc.perform(delete("/v1/budgets/" + budgetId)
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/v1/budgets/" + budgetId)
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isNotFound());
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

    private Long createBudget(String bearer, String period, Double amount, BudgetType type,
                              Long categoryId, Long userId) throws Exception {
        var budgetData = new java.util.HashMap<String, Object>();
        budgetData.put("period", period);
        budgetData.put("amount", amount);
        budgetData.put("type", type.name());
        budgetData.put("allowRollover", false);

        if (categoryId != null) {
            budgetData.put("categoryId", categoryId);
        }
        if (userId != null) {
            budgetData.put("userId", userId);
        }

        var res = mvc.perform(post("/v1/budgets")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(budgetData)))
                .andExpect(status().isOk())
                .andReturn();
        return read(res).get("id").asLong();
    }

    private JsonNode read(MvcResult mvcResult) throws Exception {
        return om.readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
