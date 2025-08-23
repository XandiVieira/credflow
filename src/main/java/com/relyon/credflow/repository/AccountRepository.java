package com.relyon.credflow.repository;

import com.relyon.credflow.model.account.Account;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {

    @EntityGraph(attributePaths = "users")
    List<Account> findAll();
}