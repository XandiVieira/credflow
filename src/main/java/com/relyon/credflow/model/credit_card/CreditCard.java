package com.relyon.credflow.model.credit_card;

import com.relyon.credflow.model.BaseEntity;
import com.relyon.credflow.model.account.Account;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "credit_card",
        uniqueConstraints = @UniqueConstraint(columnNames = {"nickname", "account_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class CreditCard extends BaseEntity {

    @ToString.Include
    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String tier;

    @Column(nullable = false)
    private String issuer;

    @Column(name = "last_four_digits", length = 4, nullable = false)
    private String lastFourDigits;

    @Column(name = "closing_day", nullable = false)
    private Integer closingDay;

    @Column(name = "due_day", nullable = false)
    private Integer dueDay;

    @Column(name = "credit_limit", precision = 14, scale = 2, nullable = false)
    private BigDecimal creditLimit;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account account;
}
