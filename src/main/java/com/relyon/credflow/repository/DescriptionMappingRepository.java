package com.relyon.credflow.repository;

import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DescriptionMappingRepository extends JpaRepository<DescriptionMapping, Long> {
    Optional<DescriptionMapping> findByNormalizedDescriptionAndAccountId(String normalizedDescription, Long account);

    List<DescriptionMapping> findAllByAccountId(Long accountId);

    Page<DescriptionMapping> findAllByAccountId(Long accountId, Pageable pageable);

    Page<DescriptionMapping> findAllByAccountIdAndCategoryIsNull(Long accountId, Pageable pageable);

    Optional<DescriptionMapping> findByIdAndAccountId(Long id, Long accountId);
}