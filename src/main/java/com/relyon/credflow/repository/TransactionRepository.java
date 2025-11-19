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

    @EntityGraph(attributePaths = {"responsibleUsers", "category", "creditCard"})
    Optional<Transaction> findByIdAndAccountId(Long id, Long accountId);

    @EntityGraph(attributePaths = {"responsibleUsers", "category", "creditCard"})
    List<Transaction> findAll(Specification<Transaction> spec, Sort sort);

    @EntityGraph(attributePaths = {"responsibleUsers", "category", "creditCard"})
    @Query("""
            select distinct t
              from Transaction t
              left join t.category c
              left join t.responsibleUsers r
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

    @Query("""
            select t from Transaction t
             where t.account.id = :accountId
               and t.id != :excludeTransactionId
               and t.date between :startDate and :endDate
               and abs(t.value + :amount) < 0.01
               and (:creditCardId is null or t.creditCard.id = :creditCardId)
               and t.isReversal = false
            """)
    List<Transaction> findPotentialReversals(Long accountId,
                                             Long excludeTransactionId,
                                             BigDecimal amount,
                                             LocalDate startDate,
                                             LocalDate endDate,
                                             Long creditCardId);

    List<Transaction> findByCsvImportHistoryId(Long csvImportHistoryId);
}