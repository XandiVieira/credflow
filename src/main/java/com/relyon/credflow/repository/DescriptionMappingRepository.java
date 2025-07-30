package com.relyon.credflow.repository;

import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.descriptionmapping.DescriptionMapping;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DescriptionMappingRepository extends JpaRepository<DescriptionMapping, Long> {
    Optional<DescriptionMapping> findByOriginalDescriptionIgnoreCase(String desc);
    boolean existsByOriginalDescriptionIgnoreCase(String originalDescription);
    List<DescriptionMapping> findAllByAccount(Account account);
}