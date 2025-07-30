package com.relyon.credflow.model.category;

import com.relyon.credflow.model.BaseEntity;
import com.relyon.credflow.model.account.Account;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Category extends BaseEntity {

    public Category(String name, Account account) {
        this.name = name;
        this.account = account;
    }

    @Column(nullable = false, unique = true)
    private String name;

    private String defaultResponsible;

    @ManyToOne(optional = false)
    private Account account;
}