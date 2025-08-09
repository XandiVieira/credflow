package com.relyon.credflow.model.descriptionmapping;

import com.relyon.credflow.model.BaseEntity;
import com.relyon.credflow.model.account.Account;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
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
public class DescriptionMapping extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String originalDescription;
    private String simplifiedDescription;
    private String category;
    @Column(nullable = false)
    private String normalizedDescription;

    @ManyToOne(optional = false)
    private Account account;

    @Transient
    public boolean isIncomplete() {
        return StringUtils.isBlank(this.category) || StringUtils.isBlank(this.simplifiedDescription);
    }

}