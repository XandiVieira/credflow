package com.relyon.credflow.controller;

import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.account.AccountRequestDTO;
import com.relyon.credflow.model.account.AccountResponseDTO;
import com.relyon.credflow.service.AccountService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final ModelMapper modelMapper;

    @GetMapping
    public List<AccountResponseDTO> findAll() {
        return accountService.findAll().stream()
                .map(account -> modelMapper.map(account, AccountResponseDTO.class)).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponseDTO> findById(@PathVariable Long id) {
        return accountService.findByIdOptional(id)
                .map(account -> modelMapper.map(account, AccountResponseDTO.class))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AccountResponseDTO> create(@Valid @RequestBody AccountRequestDTO requestDTO) {
        var account = modelMapper.map(requestDTO, Account.class);
        var created = accountService.create(account);
        return ResponseEntity.ok(modelMapper.map(created, AccountResponseDTO.class));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody AccountRequestDTO requestDTO
    ) {
        var updatedEntity = modelMapper.map(requestDTO, Account.class);
        var updated = accountService.update(id, updatedEntity);
        return ResponseEntity.ok(modelMapper.map(updated, AccountResponseDTO.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }
}