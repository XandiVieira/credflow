package com.relyon.credflow.repository;

import com.relyon.credflow.model.transaction.Transaction;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByChecksum(String checksum);

    List<Transaction> findByAccountIdAndDescriptionIgnoreCase(Long accountId, String originalDescription);

    Optional<Transaction> findByIdAndAccountId(Long id, Long accountId);

    @EntityGraph(attributePaths = {"responsibles", "category"})
    @Query("""
            select distinct t
              from Transaction t
              left join t.category c
              left join t.responsibles r
             where t.account.id = :accountId
               and (:fromDate   is null or t.date  >= :fromDate)
               and (:toDate     is null or t.date  <= :toDate)
               and (:minAmount  is null or t.value >= :minAmount)
               and (:maxAmount  is null or t.value <= :maxAmount)
               and (:descPattern is null or lower(t.description)          like :descPattern)
               and (:simpPattern is null or lower(t.simplifiedDescription) like :simpPattern)
               and (:categoryIds        is null or c.id in (:categoryIds))
               and (:responsibleUserIds is null or r.id in (:responsibleUserIds))
            """)
    List<Transaction> search(Long accountId,
                             String descPattern,
                             String simpPattern,
                             LocalDate fromDate,
                             LocalDate toDate,
                             BigDecimal minAmount,
                             BigDecimal maxAmount,
                             List<Long> responsibleUserIds,
                             List<Long> categoryIds,
                             Sort sort);
}