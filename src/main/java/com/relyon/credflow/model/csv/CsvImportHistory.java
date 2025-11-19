package com.relyon.credflow.model.csv;

import com.relyon.credflow.model.BaseEntity;
import com.relyon.credflow.model.account.Account;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "csv_import_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CsvImportHistory extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CsvImportFormat format;

    @Column(nullable = false)
    private Integer totalRows;

    @Column(nullable = false)
    private Integer importedRows;

    @Column(nullable = false)
    private Integer skippedRows;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CsvImportStatus status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}
