package com.relyon.credflow.repository;

import com.relyon.credflow.model.transaction.Transaction;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    boolean existsByChecksum(String checksum);

    List<Transaction> findByAccountIdAndDescriptionIgnoreCase(Long accountId, String originalDescription);

    Optional<Transaction> findByIdAndAccountId(Long id, Long accountId);

    @EntityGraph(attributePaths = {"responsibles", "category"})
    List<Transaction> findAll(Specification<Transaction> spec, Sort sort);
}