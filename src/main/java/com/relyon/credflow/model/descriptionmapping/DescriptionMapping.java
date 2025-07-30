package com.relyon.credflow.model.descriptionmapping;

import com.relyon.credflow.model.BaseEntity;
import com.relyon.credflow.model.account.Account;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DescriptionMapping extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String originalDescription;
    private String simplifiedDescription;
    private String category;

    @ManyToOne(optional = false)
    private Account account;
}