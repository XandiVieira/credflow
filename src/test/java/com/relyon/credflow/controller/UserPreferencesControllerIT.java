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
class UserPreferencesControllerIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;

    @Test
    void getPreferences_newUser_shouldReturnDefaults() throws Exception {
        var ctx = registerAndLogin("user_prefs_default");

        mvc.perform(get("/v1/users/preferences")
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").exists())
                .andExpect(jsonPath("$.language").exists());
    }

    @Test
    void updatePreferences_shouldSucceed() throws Exception {
        var ctx = registerAndLogin("user_prefs_update");

        mvc.perform(put("/v1/users/preferences")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "currency", "EUR",
                                "language", "de",
                                "theme", "dark",
                                "emailNotifications", true,
                                "pushNotifications", false,
                                "monthlyReport", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.language").value("de"))
                .andExpect(jsonPath("$.theme").value("dark"))
                .andExpect(jsonPath("$.emailNotifications").value(true))
                .andExpect(jsonPath("$.pushNotifications").value(false))
                .andExpect(jsonPath("$.monthlyReport").value(true));
    }

    @Test
    void updatePreferences_partialUpdate_shouldSucceed() throws Exception {
        var ctx = registerAndLogin("user_prefs_partial");

        mvc.perform(put("/v1/users/preferences")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "currency", "BRL",
                                "language", "pt",
                                "theme", "light",
                                "emailNotifications", false,
                                "pushNotifications", false,
                                "monthlyReport", false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.language").value("pt"));
    }

    @Test
    void getPreferences_afterUpdate_shouldReturnUpdatedValues() throws Exception {
        var ctx = registerAndLogin("user_prefs_get_after_update");

        mvc.perform(put("/v1/users/preferences")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "currency", "USD",
                                "language", "en",
                                "theme", "system",
                                "emailNotifications", true,
                                "pushNotifications", true,
                                "monthlyReport", true
                        ))))
                .andExpect(status().isOk());

        mvc.perform(get("/v1/users/preferences")
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.language").value("en"))
                .andExpect(jsonPath("$.theme").value("system"));
    }

    @Test
    void deletePreferences_shouldResetToDefaults() throws Exception {
        var ctx = registerAndLogin("user_prefs_delete");

        mvc.perform(put("/v1/users/preferences")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "currency", "JPY",
                                "language", "ja",
                                "theme", "dark",
                                "emailNotifications", false,
                                "pushNotifications", false,
                                "monthlyReport", false
                        ))))
                .andExpect(status().isOk());

        mvc.perform(delete("/v1/users/preferences")
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isNoContent());
    }

    @Test
    void endpoints_withoutAuth_shouldReturn401() throws Exception {
        mvc.perform(get("/v1/users/preferences"))
                .andExpect(status().isUnauthorized());

        mvc.perform(put("/v1/users/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(delete("/v1/users/preferences"))
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
