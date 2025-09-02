package com.relyon.credflow.repository;

import com.relyon.credflow.model.account.Account;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    @Override
    @EntityGraph(attributePaths = "users")
    Optional<Account> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "users")
    List<Account> findAll();

    @EntityGraph(attributePaths = "users")
    Optional<Account> findByInviteCode(String code);
}