package com.relyon.credflow.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relyon.credflow.model.user.AuthRequest;
import com.relyon.credflow.model.user.UserRequestDTO;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.open-in-view=true"
})
class UserControllerIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;

    record AuthCtx(String bearer, long userId, long accountId, String email) {
    }

    private AuthCtx registerAndLogin(String emailPrefix) throws Exception {
        var email = emailPrefix + "+" + System.nanoTime() + "@example.com";

        var reg = new UserRequestDTO();
        reg.setName("User IT");
        reg.setEmail(email);
        reg.setPassword("Str0ngP@ss!");
        reg.setConfirmPassword("Str0ngP@ss!");

        var regRes = mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        var regJson = om.readTree(regRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
        var userId = regJson.get("id").asLong();
        var accountId = regJson.get("accountId").asLong();

        var auth = new AuthRequest(email, "Str0ngP@ss!");
        var loginRes = mvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        var token = om.readTree(loginRes.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("token").asText();

        return new AuthCtx("Bearer " + token, userId, accountId, email);
    }

    @Test
    void findAll_returns200_andContainsRegisteredUser() throws Exception {
        var ctx = registerAndLogin("user_it");

        mvc.perform(get("/v1/users")
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", Matchers.hasSize(Matchers.greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content..email", Matchers.hasItem(ctx.email())));
    }

    @Test
    void findById_returns200_andBody() throws Exception {
        var ctx = registerAndLogin("user_it");

        mvc.perform(get("/v1/users/{id}", ctx.userId())
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) ctx.userId()))
                .andExpect(jsonPath("$.email").value(ctx.email()))
                .andExpect(jsonPath("$.accountId").value((int) ctx.accountId()));
    }

    @Test
    void update_existing_returns200_withUpdatedFields() throws Exception {
        var ctx = registerAndLogin("user_it");

        var req = new UserRequestDTO();
        req.setName("Updated Name");
        req.setEmail(ctx.email());
        req.setPassword("Str0ngP@ss!");
        req.setConfirmPassword("Str0ngP@ss!");

        mvc.perform(put("/v1/users/{id}", ctx.userId())
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) ctx.userId()))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.email").value(ctx.email()));
    }

    @Test
    void update_validationErrors_returns400() throws Exception {
        var ctx = registerAndLogin("user_it");

        var bad = """
                {
                  "name": "",
                  "email": "not-an-email",
                  "password": "weak",
                  "confirmPassword": "different"
                }
                """;

        mvc.perform(put("/v1/users/{id}", ctx.userId())
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bad))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void delete_existing_returns204_then404_onFetch() throws Exception {
        var ctx = registerAndLogin("user_it");

        mvc.perform(delete("/v1/users/{id}", ctx.userId())
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isNoContent());

        var inspector = registerAndLogin("inspector");

        mvc.perform(get("/v1/users/{id}", ctx.userId())
                        .header("Authorization", inspector.bearer()))
                .andExpect(status().isNotFound());
    }

    private JsonNode read(MvcResult mvcResult) throws Exception {
        return om.readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void smoke_register_helperProducesBearerAndIds() throws Exception {
        var ctx = registerAndLogin("user_it");
        assertThat(ctx.userId()).isPositive();
        assertThat(ctx.accountId()).isPositive();
        assertThat(ctx.bearer()).startsWith("Bearer ");
    }
}