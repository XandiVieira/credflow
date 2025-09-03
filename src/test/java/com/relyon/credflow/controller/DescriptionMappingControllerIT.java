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
        "spring.jpa.open-in-view=true",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true"
})
class DescriptionMappingControllerIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;

    record AuthCtx(String bearer, long userId, long accountId) {
    }

    private AuthCtx registerAndLogin(String emailPrefix) throws Exception {
        var email = emailPrefix + "+" + System.nanoTime() + "@example.com";

        var reg = new UserRequestDTO();
        reg.setName("DM IT");
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

        var login = new AuthRequest(email, "Str0ngP@ss!");
        var loginRes = mvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        var token = om.readTree(loginRes.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("token").asText();

        return new AuthCtx("Bearer " + token, userId, accountId);
    }

    private long createCategory(String name, String bearer) throws Exception {
        var body = """
                {"name":"%s"}
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

    private long createOneMapping(String original, String simplified, long categoryId, String bearer) throws Exception {
        var payload = """
                [{
                  "originalDescription":"%s",
                  "simplifiedDescription":"%s",
                  "categoryId": %d
                }]
                """.formatted(original, simplified, categoryId);

        var res = mvc.perform(post("/v1/description-mappings")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].originalDescription").value(original))
                .andExpect(jsonPath("$[0].simplifiedDescription").value(simplified))
                .andReturn();

        return om.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get(0).get("id").asLong();
    }

    @Test
    void create_returns200_andListOfDtos() throws Exception {
        var ctx = registerAndLogin("dm_it");
        var catId = createCategory("Groceries", ctx.bearer());

        var id = createOneMapping("Super-MARKET 123", "Super Market", catId, ctx.bearer());
        assertThat(id).isPositive();
    }

    @Test
    void findById_returns200_forOwnerAccount() throws Exception {
        var ctx = registerAndLogin("dm_it");
        var catId = createCategory("Transport", ctx.bearer());
        var id = createOneMapping("UBER", "Uber", catId, ctx.bearer());

        mvc.perform(get("/v1/description-mappings/{id}", id)
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) id))
                .andExpect(jsonPath("$.category").value("Transport"))
                .andExpect(jsonPath("$.accountId").value((int) ctx.accountId()));
    }

    @Test
    void findAll_returnsAll_and_onlyIncompleteFilters() throws Exception {
        var ctx = registerAndLogin("dm_it");
        var catId = createCategory("Bills", ctx.bearer());

        createOneMapping("Light Bill", "Energy", catId, ctx.bearer());
        createOneMapping("Water Bill", "Water", catId, ctx.bearer());

        mvc.perform(get("/v1/description-mappings")
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(Matchers.greaterThanOrEqualTo(2))));

        mvc.perform(get("/v1/description-mappings")
                        .header("Authorization", ctx.bearer())
                        .param("onlyIncomplete", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(0)));
    }

    @Test
    void findByOriginalDescription_isNormalizationAware() throws Exception {
        var ctx = registerAndLogin("dm_it");
        var catId = createCategory("Leisure", ctx.bearer());
        var id = createOneMapping("  Cine–TOP  ", "Cinema", catId, ctx.bearer());

        mvc.perform(get("/v1/description-mappings/by-description")
                        .header("Authorization", ctx.bearer())
                        .param("originalDescription", "cine top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) id))
                .andExpect(jsonPath("$.simplifiedDescription").value("Cinema"));
    }

    @Test
    void update_existing_changesFieldsAndCategory() throws Exception {
        var ctx = registerAndLogin("dm_it");
        var catA = createCategory("Food", ctx.bearer());
        var catB = createCategory("Restaurants", ctx.bearer());
        var id = createOneMapping("Burger King", "BK", catA, ctx.bearer());

        var patch = """
                {
                  "originalDescription":"Burger King BR",
                  "simplifiedDescription":"BurgerKing",
                  "categoryId": %d
                }
                """.formatted(catB);

        mvc.perform(put("/v1/description-mappings/{id}", id)
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) id))
                .andExpect(jsonPath("$.simplifiedDescription").value("BurgerKing"))
                .andExpect(jsonPath("$.category").value("Restaurants"));
    }

    @Test
    void delete_existing_returns204_then404_onFetch() throws Exception {
        var ctx = registerAndLogin("dm_it");
        var catId = createCategory("Apps", ctx.bearer());
        var id = createOneMapping("Google Play", "Play Store", catId, ctx.bearer());

        mvc.perform(delete("/v1/description-mappings/{id}", id)
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/v1/description-mappings/{id}", id)
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_duplicate_isIgnored_returnsEmptyList() throws Exception {
        var ctx = registerAndLogin("dm_it");
        var catId = createCategory("Transport", ctx.bearer());

        createOneMapping("Uber *Trip*", "Uber", catId, ctx.bearer());

        var payload = """
                [{
                  "originalDescription":"Uber Trip",
                  "simplifiedDescription":"Uber",
                  "categoryId": %d
                }]
                """.formatted(catId);

        mvc.perform(post("/v1/description-mappings")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(0)));
    }

    @Test
    void validation_errors_onCreate_missingFields() throws Exception {
        var ctx = registerAndLogin("dm_it");
        var payload = """
                [{
                  "originalDescription":"",
                  "simplifiedDescription":"",
                  "categoryId": null
                }]
                """;

        mvc.perform(post("/v1/description-mappings")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void crossAccount_access_yields404_forOtherUser() throws Exception {
        var owner = registerAndLogin("dm_owner");
        var other = registerAndLogin("dm_other");

        var catOwner = createCategory("OwnerCat", owner.bearer());
        var id = createOneMapping("Owner Map", "Owner", catOwner, owner.bearer());

        mvc.perform(get("/v1/description-mappings/{id}", id)
                        .header("Authorization", other.bearer()))
                .andExpect(status().isNotFound());

        var patch = """
                {
                  "originalDescription":"changed",
                  "simplifiedDescription":"changed",
                  "categoryId": %d
                }
                """.formatted(catOwner);

        mvc.perform(put("/v1/description-mappings/{id}", id)
                        .header("Authorization", other.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patch))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/v1/description-mappings/{id}", id)
                        .header("Authorization", other.bearer()))
                .andExpect(status().isNotFound());
    }
}