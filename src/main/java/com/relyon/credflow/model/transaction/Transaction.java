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
@Table(name = "transaction")
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction extends BaseEntity {

    public Transaction(LocalDate date, String description, String simplifiedDescription, Category category, BigDecimal value, Set<User> responsibleUsers) {
        this.date = date;
        this.description = description;
        this.simplifiedDescription = simplifiedDescription;
        this.category = category;
        this.value = value;
        this.responsibleUsers = responsibleUsers;
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
    private Set<User> responsibleUsers = new HashSet<>();
    @Column(name = "checksum", unique = true)
    private String checksum;
    @ManyToOne(optional = false)
    private Account account;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_card_id")
    private CreditCard creditCard;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType = TransactionType.ONE_TIME;

    private Integer currentInstallment;

    private Integer totalInstallments;

    @Column(name = "installment_group_id")
    private String installmentGroupId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionSource source = TransactionSource.MANUAL;

    @Column(name = "import_batch_id")
    private String importBatchId;

    @Column(name = "was_edited_after_import", nullable = false)
    private Boolean wasEditedAfterImport = false;

    @Column(name = "original_checksum")
    private String originalChecksum;

    @Column(name = "is_reversal", nullable = false)
    private Boolean isReversal = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_transaction_id")
    private Transaction relatedTransaction;
}