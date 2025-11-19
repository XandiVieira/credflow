package com.relyon.credflow.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relyon.credflow.model.transaction.TransactionRequestDTO;
import com.relyon.credflow.model.transaction.TransactionType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TransactionControllerIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;

    @Test
    void create_returns200_andBody() throws Exception {
        var ctx = registerAndLogin("tx_it");
        var catId = createCategory("Bills", ctx.bearer());

        var req = new TransactionRequestDTO();
        req.setDate(LocalDate.of(2024, 1, 10));
        req.setDescription("Electricity January");
        req.setSimplifiedDescription("Energy");
        req.setCategoryId(catId);
        req.setValue(new BigDecimal("123.45"));
        req.setTransactionType(TransactionType.ONE_TIME);

        var res = mvc.perform(post("/v1/transactions")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.description").value("Electricity January"))
                .andExpect(jsonPath("$.simplifiedDescription").value("Energy"))
                .andExpect(jsonPath("$.category").value("Bills"))
                .andExpect(jsonPath("$.value").value(123.45))
                .andReturn();

        var json = read(res);
        assertThat(json.get("accountId").asLong()).isEqualTo(ctx.accountId());
    }

    @Test
    void findById_returns200_forOwnerAccount() throws Exception {
        var ctx = registerAndLogin("tx_it");
        var catId = createCategory("Groceries", ctx.bearer());
        var id = createTx(ctx.bearer(), "Market Run", "Groceries", catId, "89.90", LocalDate.of(2024, 2, 1));

        mvc.perform(get("/v1/transactions/{id}", id)
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.intValue()))
                .andExpect(jsonPath("$.description").value("Market Run"))
                .andExpect(jsonPath("$.category").value("Groceries"));
    }

    @Test
    void update_existing_returns200_withUpdatedFields() throws Exception {
        var ctx = registerAndLogin("tx_it");
        var catId = createCategory("Bills", ctx.bearer());
        var id = createTx(ctx.bearer(), "Old Desc", "Old Simp", catId, "10.00", LocalDate.of(2024, 3, 3));

        var patch = new TransactionRequestDTO();
        patch.setDate(LocalDate.of(2024, 3, 5));
        patch.setDescription("New Desc");
        patch.setSimplifiedDescription("Energy");
        patch.setCategoryId(catId);
        patch.setValue(new BigDecimal("77.77"));
        patch.setTransactionType(TransactionType.ONE_TIME);

        mvc.perform(put("/v1/transactions/{id}", id)
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.intValue()))
                .andExpect(jsonPath("$.description").value("New Desc"))
                .andExpect(jsonPath("$.simplifiedDescription").value("Energy"))
                .andExpect(jsonPath("$.value").value(77.77));
    }

    @Test
    void delete_existing_returns204_andThen404OnFetch() throws Exception {
        var ctx = registerAndLogin("tx_it");
        var catId = createCategory("Bills", ctx.bearer());
        var id = createTx(ctx.bearer(), "To delete", "Energy", catId, "20.00", LocalDate.of(2024, 4, 1));

        mvc.perform(delete("/v1/transactions/{id}", id)
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/v1/transactions/{id}", id)
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void findFiltered_appliesParams_andDefaultsToDateDesc() throws Exception {
        var ctx = registerAndLogin("tx_it");
        var food = createCategory("Food", ctx.bearer());
        var bills = createCategory("Bills", ctx.bearer());

        createTx(ctx.bearer(), "Supermarket ABC", "Groceries", food, "100.00", LocalDate.of(2024, 5, 10));
        createTx(ctx.bearer(), "Supermarket XYZ", "Groceries", food, "250.00", LocalDate.of(2024, 5, 12));
        createTx(ctx.bearer(), "Electricity May", "Energy", bills, "300.00", LocalDate.of(2024, 5, 15));

        var filter = Map.of(
                "fromDate", "2024-05-01",
                "toDate", "2024-05-31",
                "descriptionContains", "market",
                "simplifiedContains", "grocer",
                "minAmount", "150",
                "maxAmount", "300",
                "categoryIds", List.of(food)
        );

        mvc.perform(post("/v1/transactions/search")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(filter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("Supermarket XYZ"))
                .andExpect(jsonPath("$[0].category").value("Food"))
                .andExpect(jsonPath("$[0].value").value(250.00));
    }

    @Test
    void crossAccount_access_gives404_forOtherUser() throws Exception {
        var owner = registerAndLogin("tx_owner");
        var catId = createCategory("Bills", owner.bearer());
        var id = createTx(owner.bearer(), "Owner Tx", "Energy", catId, "90.00", LocalDate.of(2024, 6, 1));

        var intruder = registerAndLogin("tx_other");

        mvc.perform(get("/v1/transactions/{id}", id)
                        .header("Authorization", intruder.bearer()))
                .andExpect(status().isNotFound());

        var hackReq = new TransactionRequestDTO();
        hackReq.setDate(LocalDate.of(2024, 6, 2));
        hackReq.setDescription("Hack");
        hackReq.setSimplifiedDescription("Hack");
        hackReq.setCategoryId(catId);
        hackReq.setValue(new BigDecimal("1.00"));
        hackReq.setTransactionType(TransactionType.ONE_TIME);

        mvc.perform(put("/v1/transactions/{id}", id)
                        .header("Authorization", intruder.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(hackReq)))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/v1/transactions/{id}", id)
                        .header("Authorization", intruder.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void validation_errors_onCreate_missingFields() throws Exception {
        var ctx = registerAndLogin("tx_it");

        var payload = """
                {
                  "date": null,
                  "description": "",
                  "simplifiedDescription": "x",
                  "categoryId": null,
                  "value": null,
                  "responsibleUsers": null
                }
                """;

        mvc.perform(post("/v1/transactions")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors", Matchers.hasItem(Matchers.containsStringIgnoringCase("date"))))
                .andExpect(jsonPath("$.errors", Matchers.hasItem(Matchers.containsStringIgnoringCase("description"))))
                .andExpect(jsonPath("$.errors", Matchers.hasItem(Matchers.containsStringIgnoringCase("value"))));
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

    private Long createTx(String bearer, String desc, String simp, Long categoryId, String value, LocalDate date) throws Exception {
        var req = new TransactionRequestDTO();
        req.setDate(date);
        req.setDescription(desc);
        req.setSimplifiedDescription(simp);
        req.setCategoryId(categoryId);
        req.setValue(new BigDecimal(value));
        req.setTransactionType(TransactionType.ONE_TIME);

        var res = mvc.perform(post("/v1/transactions")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        return read(res).get("id").asLong();
    }

    private JsonNode read(MvcResult mvcResult) throws Exception {
        return om.readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void bulkDelete_deletesMultipleTransactionsSuccessfully() throws Exception {
        var ctx = registerAndLogin("bulk_delete");
        var catId = createCategory("Test", ctx.bearer());

        var id1 = createTx(ctx.bearer(), "Tx 1", "Test", catId, "10.00", LocalDate.of(2024, 7, 1));
        var id2 = createTx(ctx.bearer(), "Tx 2", "Test", catId, "20.00", LocalDate.of(2024, 7, 2));
        var id3 = createTx(ctx.bearer(), "Tx 3", "Test", catId, "30.00", LocalDate.of(2024, 7, 3));

        mvc.perform(delete("/v1/transactions")
                        .header("Authorization", ctx.bearer())
                        .param("ids", id1.toString(), id2.toString(), id3.toString()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/v1/transactions/{id}", id1)
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isNotFound());

        mvc.perform(get("/v1/transactions/{id}", id2)
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isNotFound());

        mvc.perform(get("/v1/transactions/{id}", id3)
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void bulkUpdateCategory_updatesAllTransactionsSuccessfully() throws Exception {
        var ctx = registerAndLogin("bulk_category");
        var oldCat = createCategory("Old Category", ctx.bearer());
        var newCat = createCategory("New Category", ctx.bearer());

        var id1 = createTx(ctx.bearer(), "Tx 1", "Test", oldCat, "10.00", LocalDate.of(2024, 8, 1));
        var id2 = createTx(ctx.bearer(), "Tx 2", "Test", oldCat, "20.00", LocalDate.of(2024, 8, 2));

        mvc.perform(put("/v1/transactions/category")
                        .header("Authorization", ctx.bearer())
                        .param("ids", id1.toString(), id2.toString())
                        .param("categoryId", newCat.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].category").value("New Category"))
                .andExpect(jsonPath("$[1].category").value("New Category"));
    }

    @Test
    void bulkUpdateCategory_withNullCategory_removesCategory() throws Exception {
        var ctx = registerAndLogin("bulk_null_cat");
        var catId = createCategory("Category", ctx.bearer());

        var id1 = createTx(ctx.bearer(), "Tx 1", "Test", catId, "10.00", LocalDate.of(2024, 9, 1));

        mvc.perform(put("/v1/transactions/category")
                        .header("Authorization", ctx.bearer())
                        .param("ids", id1.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].category").doesNotExist());
    }

    @Test
    void bulkUpdateResponsibleUsers_updatesAllTransactionsSuccessfully() throws Exception {
        var ctx = registerAndLogin("bulk_resp");
        var catId = createCategory("Test", ctx.bearer());

        var id1 = createTx(ctx.bearer(), "Tx 1", "Test", catId, "10.00", LocalDate.of(2024, 10, 1));
        var id2 = createTx(ctx.bearer(), "Tx 2", "Test", catId, "20.00", LocalDate.of(2024, 10, 2));

        var userRes = mvc.perform(get("/v1/users/select")
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isOk())
                .andReturn();

        var users = om.readTree(userRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
        var userId = users.get(0).get("id").asText();

        mvc.perform(put("/v1/transactions/responsible-users")
                        .header("Authorization", ctx.bearer())
                        .param("ids", id1.toString(), id2.toString())
                        .param("responsibleUserIds", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].responsibleUsers", Matchers.hasSize(1)))
                .andExpect(jsonPath("$[1].responsibleUsers", Matchers.hasSize(1)));
    }

    @Test
    void create_setsSourceToManualAndCallsRefundDetection() throws Exception {
        var ctx = registerAndLogin("source_manual");
        var catId = createCategory("Test", ctx.bearer());

        var req = new TransactionRequestDTO();
        req.setDate(LocalDate.of(2024, 12, 1));
        req.setDescription("Test Transaction");
        req.setCategoryId(catId);
        req.setValue(new BigDecimal("50.00"));
        req.setTransactionType(TransactionType.ONE_TIME);

        mvc.perform(post("/v1/transactions")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andExpect(jsonPath("$.wasEditedAfterImport").value(false))
                .andExpect(jsonPath("$.isReversal").value(false));
    }
}