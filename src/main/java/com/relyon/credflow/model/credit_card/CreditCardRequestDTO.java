package com.relyon.credflow.model.credit_card;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreditCardRequestDTO {

    private String nickname;
    private String brand;
    private String tier;
    private String issuer;
    private String lastFourDigits;
    private Integer closingDay;
    private Integer dueDay;
    private BigDecimal creditLimit;
    private Long holderId;

}
