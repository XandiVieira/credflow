package com.relyon.credflow.model.category;

import com.relyon.credflow.model.BaseEntity;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "category",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "account_id"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Category extends BaseEntity {

    @ToString.Include
    @Column(nullable = false)
    private String name;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "category_default_responsibles",
            joinColumns = @JoinColumn(name = "category_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"category_id", "user_id"})
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<User> defaultResponsibles = new HashSet<>();

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account account;

    public Category(String name, Account account) {
        this.name = name;
        this.account = account;
    }
}
