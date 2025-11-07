package com.relyon.credflow.model.transaction;

import com.relyon.credflow.model.BaseEntity;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.credit_card.CreditCard;
import com.relyon.credflow.model.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction extends BaseEntity {

    public Transaction(LocalDate date, String description, String simplifiedDescription, Category category, BigDecimal value, Set<User> responsibles) {
        this.date = date;
        this.description = description;
        this.simplifiedDescription = simplifiedDescription;
        this.category = category;
        this.value = value;
        this.responsibles = responsibles;
    }

    private LocalDate date;
    private String description;
    private String simplifiedDescription;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
    private BigDecimal value;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "transaction_responsibles",
            joinColumns = @JoinColumn(name = "transaction_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"transaction_id", "user_id"})
    )
    private Set<User> responsibles = new HashSet<>();
    @Column(name = "checksum", unique = true)
    private String checksum;
    @ManyToOne(optional = false)
    private Account account;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_card_id")
    private CreditCard creditCard;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'EVENTUAL'")
    private TransactionType transactionType;

    private Integer currentInstallment;

    private Integer totalInstallments;

    @Column(name = "installment_group_id")
    private String installmentGroupId;
}