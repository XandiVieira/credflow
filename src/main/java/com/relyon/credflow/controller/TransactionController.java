package com.relyon.credflow.controller;

import com.relyon.credflow.model.transaction.Transaction;
import com.relyon.credflow.model.transaction.TransactionRequestDTO;
import com.relyon.credflow.model.transaction.TransactionResponseDTO;
import com.relyon.credflow.repository.DescriptionMappingRepository;
import com.relyon.credflow.service.AccountService;
import com.relyon.credflow.service.TransactionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@AllArgsConstructor
@RequestMapping("/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final DescriptionMappingRepository descMapRepo;
    private final AccountService accountService;
    private final ModelMapper modelMapper;

    @PostMapping("/import/csv/banrisul/{accountId}")
    public ResponseEntity<List<TransactionResponseDTO>> importBanrisulCSV(
            @RequestParam("file") MultipartFile file,
            @PathVariable Long accountId
    ) {
        var transactions = transactionService.importFromBanrisulCSV(file, accountId);
        var response = transactions.stream()
                .map(t -> modelMapper.map(t, TransactionResponseDTO.class))
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<TransactionResponseDTO> create(@Valid @RequestBody TransactionRequestDTO dto) {
        var transaction = modelMapper.map(dto, Transaction.class);
        var account = accountService.findById(dto.getAccountId());
        transaction.setAccount(account);
        var created = transactionService.create(transaction);
        return ResponseEntity.ok(modelMapper.map(created, TransactionResponseDTO.class));
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponseDTO>> findFiltered(
            @RequestParam(required = false) String responsible,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "dateDesc") String sort
    ) {
        var filtered = transactionService.findFiltered(responsible, category, startDate, endDate, sort)
                .stream()
                .map(transaction -> modelMapper.map(transaction, TransactionResponseDTO.class)).toList();

        return ResponseEntity.ok(filtered);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponseDTO> findById(@PathVariable Long id) {
        return transactionService.findById(id)
                .map(transaction -> modelMapper.map(transaction, TransactionResponseDTO.class))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponseDTO> update(@PathVariable Long id, @Valid @RequestBody TransactionRequestDTO dto) {
        var transaction = modelMapper.map(dto, Transaction.class);
        transaction.setAccount(accountService.findById(dto.getAccountId()));
        var updated = transactionService.update(id, transaction);
        return ResponseEntity.ok(modelMapper.map(updated, TransactionResponseDTO.class));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}