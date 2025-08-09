package com.relyon.credflow.repository;

import com.relyon.credflow.model.category.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByAccountId(Long accountId);

    Optional<Category> findByIdAndAccountId(Long id, Long accountId);

    Optional<Category> findByNameIgnoreCaseAndAccountId(String name, Long accountId);
}