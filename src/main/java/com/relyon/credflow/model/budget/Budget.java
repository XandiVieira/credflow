package com.relyon.credflow.model.budget;

import com.relyon.credflow.model.BaseEntity;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.category.Category;
import com.relyon.credflow.model.user.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.YearMonth;

@Entity
@Table(name = "budget", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"account_id", "period", "category_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Budget extends BaseEntity {

    @ToString.Include
    @Column(nullable = false)
    private YearMonth period;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BudgetType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "allow_rollover", nullable = false)
    @Builder.Default
    private Boolean allowRollover = false;

    @Column(name = "rolled_over_amount", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal rolledOverAmount = BigDecimal.ZERO;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account account;
}
