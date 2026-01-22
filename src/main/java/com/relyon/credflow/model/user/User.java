package com.relyon.credflow.model.user;

import com.relyon.credflow.model.BaseEntity;
import com.relyon.credflow.model.account.Account;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User extends BaseEntity {

    @ManyToOne
    private Account account;

    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.MEMBER;

    private String passwordResetToken;

    private LocalDateTime resetTokenExpiry;
}