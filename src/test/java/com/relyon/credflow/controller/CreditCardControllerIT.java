package com.relyon.credflow.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relyon.credflow.model.credit_card.CreditCardRequestDTO;
import com.relyon.credflow.model.user.AuthRequest;
import com.relyon.credflow.model.user.UserRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.open-in-view=true"
})
@Transactional
class CreditCardControllerIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    record AuthCtx(String bearer, long userId, long accountId) {
    }

    private AuthCtx registerAndLogin(String emailPrefix) throws Exception {
        var email = emailPrefix + "+" + System.nanoTime() + "@example.com";

        var reg = new UserRequestDTO();
        reg.setName("Card IT");
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

    private long createCreditCard(String bearer, long holderId) throws Exception {
        return createCreditCard(bearer, holderId, "Visa Gold");
    }

    private long createCreditCard(String bearer, long holderId, String nickname) throws Exception {
        var dto = new CreditCardRequestDTO();
        dto.setNickname(nickname);
        dto.setBrand("Visa");
        dto.setTier("Gold");
        dto.setIssuer("Bank XYZ");
        dto.setLastFourDigits("1234");
        dto.setClosingDay(15);
        dto.setDueDay(25);
        dto.setCreditLimit(new BigDecimal("10000.00"));
        dto.setHolderId(holderId);

        var res = mvc.perform(post("/v1/credit-cards")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        JsonNode json = om.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return json.get("id").asLong();
    }

    @Test
    void create_whenValidRequest_shouldReturn200WithDTO() throws Exception {
        var ctx = registerAndLogin("card_it");

        var dto = new CreditCardRequestDTO();
        dto.setNickname("Mastercard Platinum");
        dto.setBrand("Mastercard");
        dto.setTier("Platinum");
        dto.setIssuer("Bank ABC");
        dto.setLastFourDigits("5678");
        dto.setClosingDay(10);
        dto.setDueDay(20);
        dto.setCreditLimit(new BigDecimal("15000.00"));
        dto.setHolderId(ctx.userId());

        mvc.perform(post("/v1/credit-cards")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nickname").value("Mastercard Platinum"))
                .andExpect(jsonPath("$.brand").value("Mastercard"))
                .andExpect(jsonPath("$.tier").value("Platinum"))
                .andExpect(jsonPath("$.issuer").value("Bank ABC"))
                .andExpect(jsonPath("$.lastFourDigits").value("5678"))
                .andExpect(jsonPath("$.closingDay").value(10))
                .andExpect(jsonPath("$.dueDay").value(20));
    }

    @Test
    void create_whenMissingNickname_shouldReturn400() throws Exception {
        var ctx = registerAndLogin("card_it");

        var dto = new CreditCardRequestDTO();
        dto.setBrand("Visa");
        dto.setTier("Gold");
        dto.setIssuer("Bank XYZ");
        dto.setLastFourDigits("1234");
        dto.setClosingDay(15);
        dto.setDueDay(25);
        dto.setCreditLimit(new BigDecimal("10000.00"));
        dto.setHolderId(ctx.userId());

        mvc.perform(post("/v1/credit-cards")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenInvalidLastFourDigits_shouldReturn400() throws Exception {
        var ctx = registerAndLogin("card_it");

        var dto = new CreditCardRequestDTO();
        dto.setNickname("Visa Gold");
        dto.setBrand("Visa");
        dto.setTier("Gold");
        dto.setIssuer("Bank XYZ");
        dto.setLastFourDigits("12");
        dto.setClosingDay(15);
        dto.setDueDay(25);
        dto.setCreditLimit(new BigDecimal("10000.00"));
        dto.setHolderId(ctx.userId());

        mvc.perform(post("/v1/credit-cards")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenInvalidClosingDay_shouldReturn400() throws Exception {
        var ctx = registerAndLogin("card_it");

        var dto = new CreditCardRequestDTO();
        dto.setNickname("Visa Gold");
        dto.setBrand("Visa");
        dto.setTier("Gold");
        dto.setIssuer("Bank XYZ");
        dto.setLastFourDigits("1234");
        dto.setClosingDay(35);
        dto.setDueDay(25);
        dto.setCreditLimit(new BigDecimal("10000.00"));
        dto.setHolderId(ctx.userId());

        mvc.perform(post("/v1/credit-cards")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAll_whenCreditCardsExist_shouldReturnList() throws Exception {
        var ctx = registerAndLogin("card_it");
        createCreditCard(ctx.bearer(), ctx.userId(), "Visa Gold");
        createCreditCard(ctx.bearer(), ctx.userId(), "Mastercard Platinum");

        mvc.perform(get("/v1/credit-cards")
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].availableCreditLimit").exists())
                .andExpect(jsonPath("$.content[0].currentBill").exists());
    }

    @Test
    void getAll_whenNoCreditCards_shouldReturnEmptyList() throws Exception {
        var ctx = registerAndLogin("card_it");

        mvc.perform(get("/v1/credit-cards")
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void getAllSelect_shouldReturnSimplifiedList() throws Exception {
        var ctx = registerAndLogin("card_it");
        createCreditCard(ctx.bearer(), ctx.userId());

        mvc.perform(get("/v1/credit-cards/select")
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].description").exists())
                .andExpect(jsonPath("$[0].description").value("Visa Gold - 1234"));
    }

    @Test
    void getById_whenCardExists_shouldReturnCard() throws Exception {
        var ctx = registerAndLogin("card_it");
        var cardId = createCreditCard(ctx.bearer(), ctx.userId());

        mvc.perform(get("/v1/credit-cards/" + cardId)
                        .header("Authorization", ctx.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId))
                .andExpect(jsonPath("$.nickname").value("Visa Gold"))
                .andExpect(jsonPath("$.availableCreditLimit").exists())
                .andExpect(jsonPath("$.currentBill").exists())
                .andExpect(jsonPath("$.currentBill.cycleStartDate").exists())
                .andExpect(jsonPath("$.currentBill.cycleClosingDate").exists())
                .andExpect(jsonPath("$.currentBill.dueDate").exists())
                .andExpect(jsonPath("$.currentBill.totalAmount").exists());
    }

    @Test
    void getById_whenCardBelongsToDifferentAccount_shouldReturnNull() throws Exception {
        var ctx1 = registerAndLogin("card_it1");
        var ctx2 = registerAndLogin("card_it2");

        var cardId = createCreditCard(ctx1.bearer(), ctx1.userId());

        mvc.perform(get("/v1/credit-cards/" + cardId)
                        .header("Authorization", ctx2.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").doesNotExist());
    }

    @Test
    void create_whenHolderFromDifferentAccount_shouldReturn400() throws Exception {
        var ctx1 = registerAndLogin("card_it1");
        var ctx2 = registerAndLogin("card_it2");

        var dto = new CreditCardRequestDTO();
        dto.setNickname("Visa Gold");
        dto.setBrand("Visa");
        dto.setTier("Gold");
        dto.setIssuer("Bank XYZ");
        dto.setLastFourDigits("1234");
        dto.setClosingDay(15);
        dto.setDueDay(25);
        dto.setCreditLimit(new BigDecimal("10000.00"));
        dto.setHolderId(ctx2.userId());

        mvc.perform(post("/v1/credit-cards")
                        .header("Authorization", ctx1.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenNegativeCreditLimit_shouldReturn400() throws Exception {
        var ctx = registerAndLogin("card_it");

        var dto = new CreditCardRequestDTO();
        dto.setNickname("Visa Gold");
        dto.setBrand("Visa");
        dto.setTier("Gold");
        dto.setIssuer("Bank XYZ");
        dto.setLastFourDigits("1234");
        dto.setClosingDay(15);
        dto.setDueDay(25);
        dto.setCreditLimit(new BigDecimal("-1000.00"));
        dto.setHolderId(ctx.userId());

        mvc.perform(post("/v1/credit-cards")
                        .header("Authorization", ctx.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }
}
