package com.relyon.credflow.model.credit_card;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;


@Data
public class CreditCardResponseDTO {

    private Long id;
    private Long account;
    private HolderDTO holder;
    private String nickname;
    private String brand;
    private String tier;
    private String issuer;
    private String lastFourDigits;
    private Integer closingDay;
    private Integer dueDay;
    private BigDecimal creditLimit;
    private BigDecimal availableCreditLimit;
    private CurrentBillDTO currentBill;

    @Data
    public static class HolderDTO {
        private Long id;
        private String name;
        private String email;
    }

    @Data
    public static class CurrentBillDTO {
        private LocalDate cycleStartDate;
        private LocalDate cycleClosingDate;
        private LocalDate dueDate;
        private BigDecimal totalAmount;
    }

}
