package com.relyon.credflow.controller;

import com.relyon.credflow.model.mapper.TransactionMapper;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.transaction.TransactionRequestDTO;
import com.relyon.credflow.model.transaction.TransactionResponseDTO;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/transactions")
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionMapper mapper;

    @PostMapping("/import/csv/banrisul")
    public ResponseEntity<List<TransactionResponseDTO>> importBanrisulCSV(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("POST /import/csv/banrisul for account {}", user.getAccountId());
        var transactions = transactionService.importFromBanrisulCSV(file, user.getAccountId());
        var response = transactions.stream().map(mapper::toDto).toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<TransactionResponseDTO> create(
            @Valid @RequestBody TransactionRequestDTO dto,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("POST /v1/transactions to create transaction for account {}", user.getAccountId());
        var transaction = mapper.toEntity(dto);
        var created = transactionService.create(transaction, user.getAccountId());
        return ResponseEntity.ok(mapper.toDto(created));
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponseDTO>> findFiltered(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String descriptionLike,
            @RequestParam(required = false) String simplifiedLike,
            @RequestParam(required = false) BigDecimal minValue,
            @RequestParam(required = false) BigDecimal maxValue,
            @RequestParam(required = false) List<Long> responsibleIds,
            @RequestParam(required = false) List<Long> categoryIds,
            @ParameterObject Sort sort,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        responsibleIds = (responsibleIds == null || responsibleIds.isEmpty()) ? null : responsibleIds;
        categoryIds = (categoryIds == null || categoryIds.isEmpty()) ? null : categoryIds;

        var effectiveSort = (sort == null || sort.isUnsorted())
                ? Sort.by(Sort.Order.desc("date"))
                : sort;

        log.info("GET /v1/transactions for account {}, filters: start={}, end={}, descLike='{}', "
                        + "simpLike='{}', min={}, max={}, responsibles={}, categories={}, sort={}",
                user.getAccountId(), startDate, endDate, descriptionLike, simplifiedLike,
                minValue, maxValue, responsibleIds, categoryIds, effectiveSort);

        var result = transactionService.findByFilters(
                user.getAccountId(),
                startDate, endDate,
                descriptionLike, simplifiedLike,
                minValue, maxValue,
                responsibleIds, categoryIds,
                sort
        ).stream().map(mapper::toDto).toList();


        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponseDTO> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("GET /v1/transactions/{} for account {}", id, user.getAccountId());
        return transactionService.findById(id, user.getAccountId())
                .map(mapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequestDTO dto,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("PUT /v1/transactions/{} for account {}", id, user.getAccountId());
        Transaction patch = mapper.toEntity(dto); // category stub; account comes from path principal
        var updated = transactionService.update(id, patch, user.getAccountId());
        return ResponseEntity.ok(mapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("DELETE /v1/transactions/{} for account {}", id, user.getAccountId());
        transactionService.delete(id, user.getAccountId());
        return ResponseEntity.noContent().build();
    }
}