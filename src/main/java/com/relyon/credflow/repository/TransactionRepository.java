package com.relyon.credflow.repository;

import com.relyon.credflow.model.transaction.Transaction;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByChecksum(String checksum);

    List<Transaction> findByAccountIdAndResponsibleAndDateBetween(Long accountId, String responsible, LocalDate start, LocalDate end, Sort sort);

    List<Transaction> findByAccountIdAndResponsibleAndCategoryAndDateBetween(Long accountId, String responsible, String category, LocalDate start, LocalDate end, Sort sort);

    List<Transaction> findByAccountIdAndDescriptionIgnoreCase(Long accountId, String originalDescription);

    List<Transaction> findByAccountId(Long accountId);

    Optional<Transaction> findByIdAndAccountId(Long id, Long accountId);
}