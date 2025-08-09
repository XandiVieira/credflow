package com.relyon.credflow.model.transaction;

import com.relyon.credflow.model.BaseEntity;
import com.relyon.credflow.model.account.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
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
    public Transaction(LocalDate date, String description, String simplifiedDescription, String category, BigDecimal value, String responsible) {
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
    private String category;
    private BigDecimal value;
    private String responsible;
    @Column(name = "checksum", unique = true, nullable = false)
    private String checksum;
    @ManyToOne(optional = false)
    private Account account;
}