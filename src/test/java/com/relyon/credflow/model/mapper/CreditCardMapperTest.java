package com.relyon.credflow.model.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.user.User;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class CreditCardMapperTest {

    private CreditCardMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(CreditCardMapper.class);
    }

    @Test
    void toDTO_withCreditCard_mapsAllFields() {
        var account = new Account();
        account.setId(1L);
        account.setName("Test Account");

        var holder = new User();
        holder.setId(10L);
        holder.setName("Card Holder");
        holder.setEmail("holder@example.com");

        var creditCard = new CreditCard();
        creditCard.setId(100L);
        creditCard.setNickname("My Card");
        creditCard.setBrand("VISA");
        creditCard.setTier("Gold");
        creditCard.setIssuer("Bank");
        creditCard.setLastFourDigits("1234");
        creditCard.setCreditLimit(BigDecimal.valueOf(5000));
        creditCard.setClosingDay(15);
        creditCard.setDueDay(25);
        creditCard.setAccount(account);
        creditCard.setHolder(holder);

        var result = mapper.toDTO(creditCard);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("My Card", result.getNickname());
        assertEquals("VISA", result.getBrand());
        assertEquals("1234", result.getLastFourDigits());
        assertEquals(BigDecimal.valueOf(5000), result.getCreditLimit());
        assertEquals(15, result.getClosingDay());
        assertEquals(25, result.getDueDay());
        assertEquals(1L, result.getAccount());
    }

    @Test
    void toDTO_mapsHolderFields() {
        var account = new Account();
        account.setId(1L);

        var holder = new User();
        holder.setId(10L);
        holder.setName("John Doe");
        holder.setEmail("john@example.com");

        var creditCard = new CreditCard();
        creditCard.setId(100L);
        creditCard.setNickname("Card");
        creditCard.setBrand("VISA");
        creditCard.setTier("Gold");
        creditCard.setIssuer("Bank");
        creditCard.setLastFourDigits("1234");
        creditCard.setCreditLimit(BigDecimal.valueOf(5000));
        creditCard.setClosingDay(15);
        creditCard.setDueDay(25);
        creditCard.setAccount(account);
        creditCard.setHolder(holder);

        var result = mapper.toDTO(creditCard);

        assertNotNull(result);
        assertNotNull(result.getHolder());
        assertEquals(10L, result.getHolder().getId());
        assertEquals("John Doe", result.getHolder().getName());
        assertEquals("john@example.com", result.getHolder().getEmail());
    }

    @Test
    void toDTO_withNullAccount_returnsNullAccountId() {
        var creditCard = new CreditCard();
        creditCard.setId(100L);
        creditCard.setAccount(null);

        var result = mapper.toDTO(creditCard);

        assertNotNull(result);
        assertNull(result.getAccount());
    }

    @Test
    void toDTO_withNullHolder_mapsNullHolder() {
        var account = new Account();
        account.setId(1L);

        var creditCard = new CreditCard();
        creditCard.setId(100L);
        creditCard.setNickname("Card");
        creditCard.setBrand("VISA");
        creditCard.setTier("Gold");
        creditCard.setIssuer("Bank");
        creditCard.setLastFourDigits("1234");
        creditCard.setCreditLimit(BigDecimal.valueOf(5000));
        creditCard.setClosingDay(15);
        creditCard.setDueDay(25);
        creditCard.setAccount(account);
        creditCard.setHolder(null);

        var result = mapper.toDTO(creditCard);

        assertNotNull(result);
        assertNull(result.getHolder());
    }

    @Test
    void toDTO_differentBrands_mapsCorrectly() {
        var account = new Account();
        account.setId(1L);

        var holder = new User();
        holder.setId(1L);

        var mastercard = new CreditCard();
        mastercard.setId(1L);
        mastercard.setNickname("Card 1");
        mastercard.setBrand("MASTERCARD");
        mastercard.setTier("Gold");
        mastercard.setIssuer("Bank");
        mastercard.setLastFourDigits("1234");
        mastercard.setCreditLimit(BigDecimal.valueOf(5000));
        mastercard.setClosingDay(15);
        mastercard.setDueDay(25);
        mastercard.setAccount(account);
        mastercard.setHolder(holder);

        var elo = new CreditCard();
        elo.setId(2L);
        elo.setNickname("Card 2");
        elo.setBrand("ELO");
        elo.setTier("Gold");
        elo.setIssuer("Bank");
        elo.setLastFourDigits("5678");
        elo.setCreditLimit(BigDecimal.valueOf(5000));
        elo.setClosingDay(15);
        elo.setDueDay(25);
        elo.setAccount(account);
        elo.setHolder(holder);

        assertEquals("MASTERCARD", mapper.toDTO(mastercard).getBrand());
        assertEquals("ELO", mapper.toDTO(elo).getBrand());
    }
}
