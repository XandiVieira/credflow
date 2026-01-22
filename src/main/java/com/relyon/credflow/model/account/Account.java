package com.relyon.credflow.model.account;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.relyon.credflow.model.BaseEntity;
import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import java.util.List;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Account extends BaseEntity {

    @JsonIgnore
    @OneToMany(mappedBy = "account")
    private List<User> users;

    @JsonIgnore
    @OneToMany(mappedBy = "account")
    private List<DescriptionMapping> descriptionMappings;

    @JsonIgnore
    @OneToMany(mappedBy = "account")
    private List<Transaction> transactions;

    @ToString.Include
    private String name;

    @ToString.Include
    private String description;

    @ToString.Include
    @Column(unique = true)
    private String inviteCode;
}
