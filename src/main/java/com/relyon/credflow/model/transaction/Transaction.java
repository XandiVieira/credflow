package com.relyon.credflow.model.transaction;

import com.relyon.credflow.model.BaseEntity;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.category.Category;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction extends BaseEntity {
    public Transaction(LocalDate date, String description, String simplifiedDescription, Category category, BigDecimal value, String responsible) {
        this.date = date;
        this.description = description;
        this.simplifiedDescription = simplifiedDescription;
        this.category = category;
        this.value = value;
        this.responsible = responsible;
    }

    private LocalDate date;
    private String description;
    private String simplifiedDescription;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
    private BigDecimal value;
    private String responsible;
    @Column(name = "checksum", unique = true, nullable = false)
    private String checksum;
    @ManyToOne(optional = false)
    private Account account;
}