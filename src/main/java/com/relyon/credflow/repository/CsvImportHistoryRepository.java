package com.relyon.credflow.repository;

import com.relyon.credflow.model.csv.CsvImportHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CsvImportHistoryRepository extends JpaRepository<CsvImportHistory, Long> {

    List<CsvImportHistory> findByAccountIdOrderByCreatedAtDesc(Long accountId);
}
