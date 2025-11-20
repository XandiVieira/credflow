package com.relyon.credflow.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relyon.credflow.configuration.TestMailConfig;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.account.AccountRequestDTO;
import com.relyon.credflow.model.user.AuthRequest;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.model.user.UserRequestDTO;
import com.relyon.credflow.repository.UserRepository;
import com.relyon.credflow.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestMailConfig.class)
class AccountControllerIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;
    @Autowired
    private AccountService accountService;
    @Autowired
    private UserRepository userRepository;

    private String bearer;
    private User authenticatedUser;

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

        authenticatedUser = userRepository.findByEmail(email).orElseThrow();

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
        var userAccount = authenticatedUser.getAccount();

        mvc.perform(get("/v1/accounts").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value(userAccount.getName()))
                .andExpect(jsonPath("$[0].id").value(userAccount.getId().intValue()));
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
                .andExpect(jsonPath("$.inviteCode", not(emptyOrNullString())));
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

    @Test
    void getInviteCode_existingAccount_shouldReturnCode() throws Exception {
        var accountId = authenticatedUser.getAccount().getId();

        mvc.perform(get("/v1/accounts/" + accountId + "/invite-code")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inviteCode").exists())
                .andExpect(jsonPath("$.inviteCode").isString());
    }

    @Test
    void regenerateInviteCode_shouldReturnNewCode() throws Exception {
        var accountId = authenticatedUser.getAccount().getId();

        var originalResponse = mvc.perform(get("/v1/accounts/" + accountId + "/invite-code")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andReturn();

        var originalCode = read(originalResponse).get("inviteCode").asText();

        var newResponse = mvc.perform(post("/v1/accounts/" + accountId + "/regenerate-invite-code")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inviteCode").exists())
                .andReturn();

        var newCode = read(newResponse).get("inviteCode").asText();

        assertThat(newCode).isNotEqualTo(originalCode);
        assertThat(newCode).hasSize(6);
    }

    @Test
    void joinAccount_validInviteCode_shouldSucceed() throws Exception {
        var accountId = authenticatedUser.getAccount().getId();

        var inviteCodeResponse = mvc.perform(get("/v1/accounts/" + accountId + "/invite-code")
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andReturn();

        var inviteCode = read(inviteCodeResponse).get("inviteCode").asText();

        var ctx2 = registerAndLogin("new_user");

        mvc.perform(post("/v1/accounts/join")
                        .header("Authorization", ctx2.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("inviteCode", inviteCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId.intValue()));
    }

    @Test
    void joinAccount_invalidInviteCode_shouldReturn404() throws Exception {
        mvc.perform(post("/v1/accounts/join")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("inviteCode", "INVALID"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void sendInvitation_shouldReturn200() throws Exception {
        var accountId = authenticatedUser.getAccount().getId();

        mvc.perform(post("/v1/accounts/" + accountId + "/send-invitation")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", "invitee@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invitation sent successfully"));
    }

    private record Ctx(String token, long accountId, long userId, String name) {
        String bearer() {
            return "Bearer " + token;
        }
    }

    private Ctx registerAndLogin(String emailPrefix) throws Exception {
        var email = emailPrefix + "+" + System.nanoTime() + "@example.com";
        var name = emailPrefix.toUpperCase();

        var register = mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "name", name,
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
        return new Ctx(token, accountId, userId, name);
    }

    private JsonNode read(MvcResult mvcResult) throws Exception {
        return om.readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}