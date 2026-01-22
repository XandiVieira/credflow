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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BudgetPreferencesControllerIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;

    @Test
    void createOrUpdate_accountLevel_shouldSucceed() throws Exception {
        var ctx = registerAndLogin("budget_prefs_account");

        mvc.perform(post("/v1/budget-preferences")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "rolloverEnabled", true,
                                "defaultBudgetAmount", 1000.00,
                                "alertThresholdPercent", 80
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.rolloverEnabled").value(true))
                .andExpect(jsonPath("$.defaultBudgetAmount").value(1000.00))
                .andExpect(jsonPath("$.alertThresholdPercent").value(80));
    }

    @Test
    void createOrUpdate_userLevel_shouldSucceed() throws Exception {
        var ctx = registerAndLogin("budget_prefs_user");

        mvc.perform(post("/v1/budget-preferences")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "userId", ctx.userId(),
                                "rolloverEnabled", false,
                                "defaultBudgetAmount", 500.00,
                                "alertThresholdPercent", 90
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(ctx.userId()))
                .andExpect(jsonPath("$.rolloverEnabled").value(false));
    }

    @Test
    void createOrUpdate_updateExisting_shouldUpdateValues() throws Exception {
        var ctx = registerAndLogin("budget_prefs_update");

        mvc.perform(post("/v1/budget-preferences")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "rolloverEnabled", true,
                                "defaultBudgetAmount", 1000.00,
                                "alertThresholdPercent", 80
                        ))))
                .andExpect(status().isOk());

        mvc.perform(post("/v1/budget-preferences")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "rolloverEnabled", false,
                                "defaultBudgetAmount", 2000.00,
                                "alertThresholdPercent", 75
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rolloverEnabled").value(false))
                .andExpect(jsonPath("$.defaultBudgetAmount").value(2000.00))
                .andExpect(jsonPath("$.alertThresholdPercent").value(75));
    }

    @Test
    void getPreferences_accountLevel_shouldReturnPreferences() throws Exception {
        var ctx = registerAndLogin("budget_prefs_get");

        mvc.perform(post("/v1/budget-preferences")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "rolloverEnabled", true,
                                "defaultBudgetAmount", 1500.00,
                                "alertThresholdPercent", 85
                        ))))
                .andExpect(status().isOk());

        mvc.perform(get("/v1/budget-preferences")
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rolloverEnabled").value(true))
                .andExpect(jsonPath("$.defaultBudgetAmount").value(1500.00));
    }

    @Test
    void getPreferences_withUserId_shouldFallbackToAccountLevel() throws Exception {
        var ctx = registerAndLogin("budget_prefs_fallback");

        mvc.perform(post("/v1/budget-preferences")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "rolloverEnabled", true,
                                "defaultBudgetAmount", 1000.00,
                                "alertThresholdPercent", 80
                        ))))
                .andExpect(status().isOk());

        mvc.perform(get("/v1/budget-preferences")
                        .header("Authorization", ctx.bearer())
                        .param("userId", String.valueOf(ctx.userId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rolloverEnabled").value(true));
    }

    @Test
    void delete_existingPreferences_shouldSucceed() throws Exception {
        var ctx = registerAndLogin("budget_prefs_delete");

        var result = mvc.perform(post("/v1/budget-preferences")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "rolloverEnabled", true,
                                "defaultBudgetAmount", 1000.00,
                                "alertThresholdPercent", 80
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        var id = read(result).get("id").asLong();

        mvc.perform(delete("/v1/budget-preferences/" + id)
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_nonExistingPreferences_shouldReturn404() throws Exception {
        var ctx = registerAndLogin("budget_prefs_delete_404");

        mvc.perform(delete("/v1/budget-preferences/99999")
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void endpoints_withoutAuth_shouldReturn401() throws Exception {
        mvc.perform(get("/v1/budget-preferences"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/v1/budget-preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(delete("/v1/budget-preferences/1"))
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

    private JsonNode read(MvcResult mvcResult) throws Exception {
        return om.readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
