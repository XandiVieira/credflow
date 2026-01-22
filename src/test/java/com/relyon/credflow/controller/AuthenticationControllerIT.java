package com.relyon.credflow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relyon.credflow.model.user.AuthRequest;
import com.relyon.credflow.model.user.UserRequestDTO;
import com.relyon.credflow.repository.AccountRepository;
import com.relyon.credflow.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(com.relyon.credflow.configuration.TestMailConfig.class)
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

    @Test
    void forgotPassword_validEmail_shouldReturn200() throws Exception {
        var email = "reset" + System.nanoTime() + "@example.com";
        registerUser(email);

        mvc.perform(post("/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        var user = userRepository.findByEmail(email).orElseThrow();
        assertThat(user.getPasswordResetToken()).isNotNull();
        assertThat(user.getResetTokenExpiry()).isNotNull();
    }

    @Test
    void forgotPassword_invalidEmail_shouldReturn404() throws Exception {
        mvc.perform(post("/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", "nonexistent@example.com"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void resetPassword_validToken_shouldReturn200AndResetPassword() throws Exception {
        var email = "reset" + System.nanoTime() + "@example.com";
        registerUser(email);

        mvc.perform(post("/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", email))))
                .andExpect(status().isOk());

        var user = userRepository.findByEmail(email).orElseThrow();
        var token = user.getPasswordResetToken();

        mvc.perform(post("/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "token", token,
                                "newPassword", "NewP@ssw0rd!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully"));

        var updatedUser = userRepository.findByEmail(email).orElseThrow();
        assertThat(updatedUser.getPasswordResetToken()).isNull();
        assertThat(updatedUser.getResetTokenExpiry()).isNull();
    }

    @Test
    void resetPassword_invalidToken_shouldReturn400() throws Exception {
        mvc.perform(post("/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "token", "invalid-token",
                                "newPassword", "NewP@ssw0rd!"
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validateResetToken_validToken_shouldReturn200() throws Exception {
        var email = "reset" + System.nanoTime() + "@example.com";
        registerUser(email);

        mvc.perform(post("/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("email", email))))
                .andExpect(status().isOk());

        var user = userRepository.findByEmail(email).orElseThrow();
        var token = user.getPasswordResetToken();

        mvc.perform(get("/v1/auth/validate-reset-token")
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token is valid"));
    }

    @Test
    void validateResetToken_invalidToken_shouldReturn400() throws Exception {
        mvc.perform(get("/v1/auth/validate-reset-token")
                        .param("token", "invalid-token"))
                .andExpect(status().isBadRequest());
    }

    private void registerUser(String email) throws Exception {
        mvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of(
                                "name", "Test User",
                                "email", email,
                                "password", "Str0ngP@ss!",
                                "confirmPassword", "Str0ngP@ss!"
                        ))))
                .andExpect(status().isCreated());
    }

    private JsonNode read(MvcResult mvcResult) throws Exception {
        return om.readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}