package com.relyon.credflow.model.account;

import com.relyon.credflow.model.BaseEntity;
import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Account extends BaseEntity {

    @OneToMany(mappedBy = "account")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private List<User> users;

    @OneToMany(mappedBy = "account")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private List<DescriptionMapping> descriptionMappings;

    @OneToMany(mappedBy = "account")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private List<Transaction> transactions;

    @ToString.Include
    private String name;

    @ToString.Include
    private String description;
}