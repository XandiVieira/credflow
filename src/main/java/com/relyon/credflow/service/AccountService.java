package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.account.Account;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;

    public List<Account> findAll() {
        log.info("Fetching all accounts");
        var accounts = accountRepository.findAll();
        log.info("Found {} accounts", accounts.size());
        return accounts;
    }

    public Account findById(Long id) {
        log.info("Fetching account by ID {}", id);
        return accountRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Account with ID {} not found", id);
                    return new ResourceNotFoundException("Account not found with ID " + id);
                });
    }

    public Account findByInviteCode(String code) {
        log.info("Fetching account by invite code {}", code);
        return accountRepository.findByInviteCode(code)
                .orElseThrow(() -> {
                    log.warn("Account with invite code {} not found", code);
                    return new ResourceNotFoundException("Account not found with invite code " + code);
                });
    }

    public Optional<Account> findByIdOptional(Long id) {
        log.info("Fetching optional account by ID {}", id);
        return accountRepository.findById(id);
    }

    public Account create(Account account) {
        log.info("Creating account: {}", account);
        var created = accountRepository.save(account);
        log.info("Account created with ID {}", created.getId());
        return created;
    }

    @Transactional
    public Account createDefaultFor(User user) {
        var name = user.getName() != null && !user.getName().isBlank()
                ? user.getName() + " Finanças"
                : "Finanças";
        var description = user.getName() != null && !user.getName().isBlank()
                ? "Finanças do(a) " + user.getName()
                : "Finanças";

        var account = new Account();
        account.setName(name);
        account.setDescription(description);
        account.setInviteCode(generateUniqueInviteCode());

        log.info("Creating default account for user {}", user.getEmail());
        return create(account);
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (accountRepository.findByInviteCode(code).isPresent());
        return code;
    }

    public Account update(Long id, Account updated) {
        log.info("Updating account with ID {}", id);
        return accountRepository.findById(id)
                .map(existing -> {
                    existing.setName(updated.getName());
                    existing.setDescription(updated.getDescription());
                    var saved = accountRepository.save(existing);
                    log.info("Account with ID {} successfully updated", id);
                    return saved;
                })
                .orElseThrow(() -> {
                    log.warn("Account with ID {} not found for update", id);
                    return new ResourceNotFoundException("Account not found with ID " + id);
                });
    }

    public void delete(Long id) {
        log.info("Deleting account with ID {}", id);
        if (!accountRepository.existsById(id)) {
            log.warn("Cannot delete. Account with ID {} not found", id);
            throw new ResourceNotFoundException("Account not found with ID " + id);
        }
        accountRepository.deleteById(id);
        log.info("Account with ID {} successfully deleted", id);
    }
}