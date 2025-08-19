package com.relyon.credflow.model.descriptionmapping;

import com.relyon.credflow.model.BaseEntity;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.category.Category;
import io.micrometer.common.util.StringUtils;
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
@Table(
        name = "descriptionMapping",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"normalizedDescription", "account_id", "category_id"}
        )
)
public class DescriptionMapping extends BaseEntity {

    @Column(nullable = false)
    private String originalDescription;

    private String simplifiedDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false)
    private String normalizedDescription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Transient
    public boolean isIncomplete() {
        return StringUtils.isBlank(this.simplifiedDescription) || this.category == null;
    }
}