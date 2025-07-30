package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.repository.AccountRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;

    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    public Account findById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID " + id));
    }

    public Account create(Account account) {
        log.info("Creating account: {}", account);
        return accountRepository.save(account);
    }

    public Account update(Long id, Account updated) {
        return accountRepository.findById(id).map(existing -> {
            existing.setName(updated.getName());
            existing.setDescription(updated.getDescription());
            return accountRepository.save(existing);
        }).orElseThrow(() -> new ResourceNotFoundException("Account not found with ID " + id));
    }

    public void delete(Long id) {
        if (!accountRepository.existsById(id)) {
            throw new ResourceNotFoundException("Account not found with ID " + id);
        }
        accountRepository.deleteById(id);
    }

    public Optional<Account> findByIdOptional(Long id) {
        return accountRepository.findById(id);
    }
}