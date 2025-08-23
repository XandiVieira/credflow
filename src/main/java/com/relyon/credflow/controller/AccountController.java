package com.relyon.credflow.controller;

import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.account.AccountRequestDTO;
import com.relyon.credflow.model.account.AccountResponseDTO;
import com.relyon.credflow.model.mapper.AccountMapper;
import com.relyon.credflow.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;
    private final AccountMapper accountMapper;

    @GetMapping
    public ResponseEntity<List<AccountResponseDTO>> findAll() {
        log.info("GET to fetch all accounts");
        var result = accountService.findAll().stream()
                .map(accountMapper::toDto)
                .toList();
        log.info("Found {} accounts", result.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponseDTO> findById(@PathVariable Long id) {
        log.info("GET to fetch account by ID {}", id);
        return accountService.findByIdOptional(id)
                .map(a -> {
                    log.info("Account with ID {} found", id);
                    return ResponseEntity.ok(accountMapper.toDto(a));
                })
                .orElseGet(() -> {
                    log.warn("Account with ID {} not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody AccountRequestDTO requestDTO) {

        log.info("PUT to update account with ID {}", id);
        Account patch = accountMapper.toEntity(requestDTO);
        var updated = accountService.update(id, patch);
        log.info("Account with ID {} successfully updated", id);
        return ResponseEntity.ok(accountMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE account with ID {}", id);
        accountService.delete(id);
        log.info("Account with ID {} successfully deleted", id);
        return ResponseEntity.noContent().build();
    }
}