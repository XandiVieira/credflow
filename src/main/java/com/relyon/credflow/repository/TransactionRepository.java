package com.relyon.credflow.repository;

import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.transaction.Transaction;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    boolean existsByChecksum(String checksum);
    @Query("SELECT t FROM Transaction t " +
            "WHERE t.responsible = :responsible " +
            "AND (:start IS NULL OR t.date >= :start) " +
            "AND (:end IS NULL OR t.date <= :end)")
    List<Transaction> findByResponsibleAndDateRange(String responsible, LocalDate start, LocalDate end, Sort sort);

    @Query("SELECT t FROM Transaction t " +
            "WHERE t.responsible = :responsible " +
            "AND t.category = :category " +
            "AND (:start IS NULL OR t.date >= :start) " +
            "AND (:end IS NULL OR t.date <= :end)")
    List<Transaction> findByResponsibleAndCategoryAndDateRange(String responsible, String category, LocalDate start, LocalDate end, Sort sort);

    List<Transaction> findByAccountAndDescriptionIgnoreCase(Account account, String originalDescription);
}