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

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.open-in-view=true"
})
class CategoryControllerIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;

    record AuthCtx(String bearer, long userId, long accountId) {
    }

    private AuthCtx registerAndLogin(String emailPrefix) throws Exception {
        var email = emailPrefix + "+" + System.nanoTime() + "@example.com";

        var reg = new UserRequestDTO();
        reg.setName("Acc IT");
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

        return new AuthCtx("Bearer " + token, userId, accountId);
    }

    private long createCategory(String name, String bearer) throws Exception {
        var body = """
                {"name":"%s","defaultResponsibleUserIds":[]}
                """.formatted(name);

        var res = mvc.perform(post("/v1/categories")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value(name))
                .andReturn();

        JsonNode json = om.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return json.get("id").asLong();
    }

    @Test
    void create_returns200_withDto() throws Exception {
        var ctx = registerAndLogin("cat_it");
        var id = createCategory("Food", ctx.bearer());
        assertThat(id).isPositive();
    }

    @Test
    void getById_returns200_forOwnerAccount() throws Exception {
        var ctx = registerAndLogin("cat_it");
        var id = createCategory("Transport", ctx.bearer());

        mvc.perform(get("/v1/categories/{id}", id)
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) id))
                .andExpect(jsonPath("$.name").value("Transport"));
    }

    @Test
    void getAll_returnsList_forOwnerAccount() throws Exception {
        var ctx = registerAndLogin("cat_it");
        createCategory("Bills", ctx.bearer());
        createCategory("Leisure", ctx.bearer());

        mvc.perform(get("/v1/categories")
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", Matchers.hasSize(Matchers.greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.content..name", Matchers.hasItems("Bills", "Leisure")));
    }

    @Test
    void update_existing_returns200_withUpdatedName() throws Exception {
        var ctx = registerAndLogin("cat_it");
        var id = createCategory("Before", ctx.bearer());

        var patch = """
                {"name":"After"}
                """;

        mvc.perform(put("/v1/categories/{id}", id)
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) id))
                .andExpect(jsonPath("$.name").value("After"));
    }

    @Test
    void delete_existing_returns204_then404_onFetch() throws Exception {
        var ctx = registerAndLogin("cat_it");
        var id = createCategory("Temp", ctx.bearer());

        mvc.perform(delete("/v1/categories/{id}", id)
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/v1/categories/{id}", id)
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_duplicateName_sameAccount_returns409() throws Exception {
        var ctx = registerAndLogin("cat_it");
        createCategory("Travel", ctx.bearer());

        var dupBody = """
                {"name":"travel"}
                """;

        mvc.perform(post("/v1/categories")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dupBody))
                .andExpect(status().isConflict());
    }

    @Test
    void create_blankName_returns400_validationError() throws Exception {
        var ctx = registerAndLogin("cat_it");

        var body = """
                {"name":""}
                """;

        mvc.perform(post("/v1/categories")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void crossAccount_access_returns404() throws Exception {
        var owner = registerAndLogin("cat_owner");
        var other = registerAndLogin("cat_other");

        var id = createCategory("OwnerOnly", owner.bearer());

        mvc.perform(get("/v1/categories/{id}", id)
                        .header("Authorization", other.bearer()))
                .andExpect(status().isNotFound());

        var patch = """
                {"name":"oops"}
                """;
        mvc.perform(put("/v1/categories/{id}", id)
                        .header("Authorization", other.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patch))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/v1/categories/{id}", id)
                        .header("Authorization", other.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_toExistingName_inSameAccount_returns409() throws Exception {
        var ctx = registerAndLogin("cat_it");
        var a = createCategory("A", ctx.bearer());
        createCategory("B", ctx.bearer());

        var patch = """
                {"name":"A"}
                """;

        mvc.perform(put("/v1/categories/{id}", a + 1) // the “B” category id
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patch))
                .andExpect(status().isConflict());
    }

    @Test
    void create_withResponsibleFromOtherAccount_returns400() throws Exception {
        var owner = registerAndLogin("cat_owner");
        var outsider = registerAndLogin("cat_out");

        var body = """
                {"name":"WithOutsider","defaultResponsibleUserIds":[%d]}
                """.formatted(outsider.userId());

        mvc.perform(post("/v1/categories")
                        .header("Authorization", owner.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}