package com.relyon.credflow.controller;

import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.mapper.TransactionMapper;
import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.transaction.TransactionRequestDTO;
import com.relyon.credflow.model.transaction.TransactionResponseDTO;
import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
        Transaction tx = mapper.toEntity(dto);              // categoryId -> Category (stub)
        var acc = new Account();
        acc.setId(user.getAccountId());
        tx.setAccount(acc);                                 // ensure account is set
        var created = transactionService.create(tx);
        return ResponseEntity.ok(mapper.toDto(created));
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponseDTO>> findFiltered(
            @RequestParam(required = false) String responsible,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "dateDesc") String sort,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("GET /v1/transactions for account {}, filters: responsible={}, category={}, startDate={}, endDate={}, sort={}",
                user.getAccountId(), responsible, category, startDate, endDate, sort);

        var filtered = transactionService
                .findFiltered(user.getAccountId(), responsible, category, startDate, endDate, sort)
                .stream()
                .map(mapper::toDto)
                .toList();

        return ResponseEntity.ok(filtered);
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