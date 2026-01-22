package com.relyon.credflow.repository;

import com.relyon.credflow.model.credit_card.CreditCard;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {

    @EntityGraph(attributePaths = {"account", "holder"})
    List<CreditCard> findAllByAccountId(Long accountId);

    @EntityGraph(attributePaths = {"account", "holder"})
    Page<CreditCard> findAllByAccountId(Long accountId, Pageable pageable);

    @EntityGraph(attributePaths = {"account", "holder"})
    Optional<CreditCard> findByIdAndAccountId(Long id, Long accountId);

    boolean existsByIdAndAccountId(Long id, Long accountId);

    @EntityGraph(attributePaths = {"account", "holder"})
    List<CreditCard> findByLastFourDigitsAndAccountId(String lastFourDigits, Long accountId);
}
