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
class InstallmentGroupControllerIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;

    @Test
    void createInstallmentGroup_shouldCreateMultipleTransactions() throws Exception {
        var ctx = registerAndLogin("installment_create");
        var categoryId = createCategory("Shopping", ctx.bearer());

        var result = mvc.perform(post("/v1/installment-groups")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "description", "TV Purchase",
                                "value", 1200.00,
                                "totalInstallments", 12,
                                "startDate", "2025-01-15",
                                "categoryId", categoryId
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.installmentGroupId").exists())
                .andExpect(jsonPath("$.totalInstallments").value(12))
                .andExpect(jsonPath("$.transactions").isArray())
                .andReturn();

        var json = read(result);
        assertThat(json.get("transactions").size()).isEqualTo(12);
    }

    @Test
    void createInstallmentGroup_withCreditCard_shouldSucceed() throws Exception {
        var ctx = registerAndLogin("installment_cc");
        var categoryId = createCategory("Electronics", ctx.bearer());
        var creditCardId = createCreditCard(ctx.bearer());

        mvc.perform(post("/v1/installment-groups")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "description", "Laptop Purchase",
                                "value", 3000.00,
                                "totalInstallments", 6,
                                "startDate", "2025-02-01",
                                "categoryId", categoryId,
                                "creditCardId", creditCardId
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactions[0].creditCard.id").value(creditCardId));
    }

    @Test
    void getAllInstallmentGroups_shouldReturnSummaries() throws Exception {
        var ctx = registerAndLogin("installment_list");
        var categoryId = createCategory("Purchases", ctx.bearer());

        createInstallmentGroup(ctx.bearer(), "Item 1", 600.00, 6, categoryId);
        createInstallmentGroup(ctx.bearer(), "Item 2", 400.00, 4, categoryId);

        var result = mvc.perform(get("/v1/installment-groups")
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isOk())
                .andReturn();

        var json = om.readTree(result.getResponse().getContentAsString());
        assertThat(json.size()).isEqualTo(2);
    }

    @Test
    void getInstallmentGroup_existingGroup_shouldReturnDetails() throws Exception {
        var ctx = registerAndLogin("installment_get");
        var categoryId = createCategory("Furniture", ctx.bearer());
        var groupId = createInstallmentGroup(ctx.bearer(), "Sofa", 2400.00, 12, categoryId);

        mvc.perform(get("/v1/installment-groups/" + groupId)
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installmentGroupId").value(groupId))
                .andExpect(jsonPath("$.description").value("Sofa"))
                .andExpect(jsonPath("$.totalInstallments").value(12))
                .andExpect(jsonPath("$.transactions").isArray());
    }

    @Test
    void getInstallmentGroup_nonExistingGroup_shouldReturn404() throws Exception {
        var ctx = registerAndLogin("installment_get_404");

        mvc.perform(get("/v1/installment-groups/non-existing-id")
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateInstallmentGroup_shouldUpdateAllTransactions() throws Exception {
        var ctx = registerAndLogin("installment_update");
        var categoryId = createCategory("Original", ctx.bearer());
        var newCategoryId = createCategory("Updated", ctx.bearer());
        var groupId = createInstallmentGroup(ctx.bearer(), "Original Description", 500.00, 5, categoryId);

        mvc.perform(put("/v1/installment-groups/" + groupId)
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "description", "Updated Description",
                                "value", 600.00,
                                "totalInstallments", 5,
                                "startDate", "2025-01-15",
                                "categoryId", newCategoryId
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated Description"));
    }

    @Test
    void updateDescription_shouldUpdateOnlyDescription() throws Exception {
        var ctx = registerAndLogin("installment_desc");
        var categoryId = createCategory("Category", ctx.bearer());
        var groupId = createInstallmentGroup(ctx.bearer(), "Old Description", 300.00, 3, categoryId);

        mvc.perform(patch("/v1/installment-groups/" + groupId + "/description")
                        .header("Authorization", ctx.bearer())
                        .param("description", "New Description"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("New Description"));
    }

    @Test
    void deleteInstallmentGroup_shouldDeleteAllTransactions() throws Exception {
        var ctx = registerAndLogin("installment_delete");
        var categoryId = createCategory("ToDelete", ctx.bearer());
        var groupId = createInstallmentGroup(ctx.bearer(), "Delete Me", 400.00, 4, categoryId);

        mvc.perform(delete("/v1/installment-groups/" + groupId)
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/v1/installment-groups/" + groupId)
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void endpoints_withoutAuth_shouldReturn401() throws Exception {
        mvc.perform(get("/v1/installment-groups"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/v1/installment-groups/some-id"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/v1/installment-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(put("/v1/installment-groups/some-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(delete("/v1/installment-groups/some-id"))
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

    private Long createCategory(String name, String bearer) throws Exception {
        var res = mvc.perform(post("/v1/categories")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isOk())
                .andReturn();
        return read(res).get("id").asLong();
    }

    private Long createCreditCard(String bearer) throws Exception {
        var res = mvc.perform(post("/v1/credit-cards")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "nickname", "Test Card",
                                "brand", "VISA",
                                "lastFourDigits", "1234",
                                "creditLimit", 10000.00,
                                "closingDay", 15,
                                "dueDay", 25
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return read(res).get("id").asLong();
    }

    private String createInstallmentGroup(String bearer, String description, Double value,
                                          Integer installments, Long categoryId) throws Exception {
        var res = mvc.perform(post("/v1/installment-groups")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "description", description,
                                "value", value,
                                "totalInstallments", installments,
                                "startDate", "2025-01-15",
                                "categoryId", categoryId
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return read(res).get("installmentGroupId").asText();
    }

    private JsonNode read(MvcResult mvcResult) throws Exception {
        return om.readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
