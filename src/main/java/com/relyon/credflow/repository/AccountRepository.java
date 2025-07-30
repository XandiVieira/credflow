package com.relyon.credflow.repository;

import com.relyon.credflow.model.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
}