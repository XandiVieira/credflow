package com.relyon.credflow.repository;

import com.relyon.credflow.model.csv.CsvImportHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CsvImportHistoryRepository extends JpaRepository<CsvImportHistory, Long> {

    List<CsvImportHistory> findByAccountIdOrderByCreatedAtDesc(Long accountId);
}
