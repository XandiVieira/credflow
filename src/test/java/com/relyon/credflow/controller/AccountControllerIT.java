package com.relyon.credflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.account.AccountRequestDTO;
import com.relyon.credflow.model.user.AuthRequest;
import com.relyon.credflow.model.user.UserRequestDTO;
import com.relyon.credflow.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountControllerIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;
    @Autowired
    private AccountService accountService;

    private String bearer;

    @BeforeEach
    void setUpAuth() throws Exception {
        var email = "acc_it+" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

        var user = new UserRequestDTO();
        user.setName("Acc IT");
        user.setEmail(email);
        user.setPassword("Str0ngP@ss!");
        user.setConfirmPassword("Str0ngP@ss!");

        mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(user)))
                .andExpect(status().isCreated());

        var auth = new AuthRequest(email, "Str0ngP@ss!");
        var res = mvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(auth)))
                .andExpect(status().isOk())
                .andReturn();

        var token = om.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("token").asText();
        bearer = "Bearer " + token;
    }

    @Test
    void findAll_returnsList_withCreatedAccounts() throws Exception {
        var a1 = new Account();
        a1.setName("One");
        a1.setDescription("First");
        a1.setInviteCode(UUID.randomUUID().toString().substring(0, 6));
        a1 = accountService.create(a1);

        var a2 = new Account();
        a2.setName("Two");
        a2.setDescription("Second");
        a2.setInviteCode(UUID.randomUUID().toString().substring(0, 6));
        a2 = accountService.create(a2);

        mvc.perform(get("/v1/accounts").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$..name", hasItems("One", "Two")))
                .andExpect(jsonPath("$..id",
                        hasItems(a1.getId().intValue(), a2.getId().intValue())));
    }

    @Test
    void findById_existing_returns200_withDto() throws Exception {
        var acc = new Account();
        acc.setName("Main");
        acc.setDescription("Primary");
        acc.setInviteCode(UUID.randomUUID().toString().substring(0, 6));
        acc = accountService.create(acc);

        mvc.perform(get("/v1/accounts/{id}", acc.getId()).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(acc.getId().intValue()))
                .andExpect(jsonPath("$.name").value("Main"))
                .andExpect(jsonPath("$.description").value("Primary"))
                .andExpect(jsonPath("$.inviteCode", not(isEmptyOrNullString())));
    }

    @Test
    void findById_missing_returns404() throws Exception {
        mvc.perform(get("/v1/accounts/{id}", Long.MAX_VALUE).header("Authorization", bearer))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_existing_returns200_withUpdatedFields() throws Exception {
        var original = new Account();
        original.setName("Before");
        original.setDescription("Old");
        original.setInviteCode(UUID.randomUUID().toString().substring(0, 6));
        original = accountService.create(original);

        var patch = new AccountRequestDTO();
        patch.setName("After");
        patch.setDescription("New");

        mvc.perform(put("/v1/accounts/{id}", original.getId())
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(original.getId().intValue()))
                .andExpect(jsonPath("$.name").value("After"))
                .andExpect(jsonPath("$.description").value("New"));
    }

    @Test
    void update_missing_returns404() throws Exception {
        var patch = new AccountRequestDTO();
        patch.setName("DoesNotMatter");
        patch.setDescription("Nope");

        mvc.perform(put("/v1/accounts/{id}", Long.MAX_VALUE)
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(patch)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_existing_returns204_andRemovesAccount() throws Exception {
        var acc = new Account();
        acc.setName("ToDelete");
        acc.setDescription("Remove me");
        acc.setInviteCode(UUID.randomUUID().toString().substring(0, 6));
        acc = accountService.create(acc);
        var id = acc.getId();

        mvc.perform(delete("/v1/accounts/{id}", id).header("Authorization", bearer))
                .andExpect(status().isNoContent());

        var thrown = false;
        try {
            accountService.findById(id);
        } catch (Exception e) {
            thrown = true;
        }
        assertThat(thrown).isTrue();
    }

    @Test
    void delete_missing_returns404() throws Exception {
        mvc.perform(delete("/v1/accounts/{id}", Long.MAX_VALUE).header("Authorization", bearer))
                .andExpect(status().isNotFound());
    }
}