package com.relyon.credflow.model.category;

import com.relyon.credflow.model.BaseEntity;
import com.relyon.credflow.model.account.Account;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "category",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "account_id"}))
public class Category extends BaseEntity {

    public Category(String name, Account account) {
        this.name = name;
        this.account = account;
    }

    @Column(nullable = false)
    private String name;

    private String defaultResponsible;

    @ManyToOne(optional = false)
    private Account account;
}