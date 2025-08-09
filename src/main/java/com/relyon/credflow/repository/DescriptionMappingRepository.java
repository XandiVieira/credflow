package com.relyon.credflow.repository;

import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DescriptionMappingRepository extends JpaRepository<DescriptionMapping, Long> {
    Optional<DescriptionMapping> findByNormalizedDescriptionAndAccountId(String normalizedDescription, Long account);

    List<DescriptionMapping> findAllByAccountId(Long accountId);

    Optional<DescriptionMapping> findByIdAndAccountId(Long id, Long accountId);
}