package com.relyon.credflow.model.category;

import com.relyon.credflow.model.BaseEntity;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "category_default_responsibles",
            joinColumns = @JoinColumn(name = "category_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"category_id", "user_id"})
    )
    private Set<User> defaultResponsibles = new HashSet<>();

    @ManyToOne(optional = false)
    private Account account;
}