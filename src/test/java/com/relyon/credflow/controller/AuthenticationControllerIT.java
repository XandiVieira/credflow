package com.relyon.credflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relyon.credflow.model.user.AuthRequest;
import com.relyon.credflow.model.user.UserRequestDTO;
import com.relyon.credflow.repository.AccountRepository;
import com.relyon.credflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthenticationControllerIT {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;

    @Test
    void register_createsUserAndAccount_returns201LocationAndBody() throws Exception {
        var req = new UserRequestDTO();
        req.setName("Polyana");
        req.setEmail("polyana@example.com");
        req.setPassword("Str0ngP@ss!");
        req.setConfirmPassword("Str0ngP@ss!");

        var res = mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/users/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/users/\\d+")))
                .andExpect(jsonPath("$.email").value("polyana@example.com"))
                .andReturn();

        var body = res.getResponse().getContentAsString(StandardCharsets.UTF_8);
        var json = om.readTree(body);
        var id = json.get("id").asLong();
        var accountId = json.get("accountId").asLong();

        var location = res.getResponse().getHeader("Location");
        assertThat(location).isEqualTo("/users/" + id);

        assertThat(accountId).isNotNull();
        assertThat(accountRepository.findById(accountId)).isPresent();
        assertThat(userRepository.findById(id)).isPresent();
    }

    @Test
    void register_passwordMismatch_returns400_withDefaultMessage() throws Exception {
        var req = new UserRequestDTO();
        req.setName("User");
        req.setEmail("user@example.com");
        req.setPassword("Str0ngP@ss!");
        req.setConfirmPassword("Str0ngP@ss!X");

        mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors", hasItem("Passwords do not match")));
    }

    @Test
    void register_weakPassword_returns400_withDefaultStrengthMessage() throws Exception {
        var req = new UserRequestDTO();
        req.setName("User");
        req.setEmail("user@example.com");
        req.setPassword("senha123");
        req.setConfirmPassword("senha123");

        mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors", hasItem(containsString("at least 8"))));
    }

    @Test
    void register_duplicateEmail_returns409_conflict() throws Exception {
        var first = new UserRequestDTO();
        first.setName("A");
        first.setEmail("dup@example.com");
        first.setPassword("Str0ngP@ss!");
        first.setConfirmPassword("Str0ngP@ss!");

        mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(first)))
                .andExpect(status().isCreated());

        var second = new UserRequestDTO();
        second.setName("B");
        second.setEmail("dup@example.com");
        second.setPassword("Str0ngP@ss!");
        second.setConfirmPassword("Str0ngP@ss!");

        mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(second)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void login_validCredentials_returns200_token() throws Exception {
        var req = new UserRequestDTO();
        req.setName("Poly");
        req.setEmail("poly@example.com");
        req.setPassword("Str0ngP@ss!");
        req.setConfirmPassword("Str0ngP@ss!");

        mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());

        var auth = new AuthRequest("poly@example.com", "Str0ngP@ss!");

        mvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void login_wrongPassword_returns401_status() throws Exception {
        var req = new UserRequestDTO();
        req.setName("Nick");
        req.setEmail("nick@example.com");
        req.setPassword("Str0ngP@ss!");
        req.setConfirmPassword("Str0ngP@ss!");

        mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());

        var bad = new AuthRequest("nick@example.com", "wrong");

        mvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(bad)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void validation_messages_default_english() throws Exception {
        var req = new UserRequestDTO();
        req.setName("");
        req.setEmail("x");
        req.setPassword("");
        req.setConfirmPassword("");

        var res = mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        var body = res.getResponse().getContentAsString(StandardCharsets.UTF_8).toLowerCase();
        assertThat(body).contains("invalid email");
        assertThat(body).contains("name is required");
        assertThat(body).contains("password is required");
        assertThat(body).contains("password confirmation is required");
    }
}