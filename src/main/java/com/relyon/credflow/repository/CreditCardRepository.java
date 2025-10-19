package com.relyon.credflow.repository;

import com.relyon.credflow.model.credit_card.CreditCard;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {

    @EntityGraph(attributePaths = "account")
    List<CreditCard> findAllByAccountId(Long accountId);
}
