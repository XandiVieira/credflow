package com.relyon.credflow.model.account;

import com.relyon.credflow.model.BaseEntity;
import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import java.util.List;
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
public class Account extends BaseEntity {

    @OneToMany(mappedBy = "account")
    private List<User> users;

    @OneToMany(mappedBy = "account")
    private List<DescriptionMapping> descriptionMappings;

    @OneToMany(mappedBy = "account")
    private List<Transaction> transactions;

    private String name;
    private String description;
}