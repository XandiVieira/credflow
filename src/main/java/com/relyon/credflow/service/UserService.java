package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User create(User user) {
        log.info("Creating user: {}", user.getEmail());
        if (user.getAccount() == null || user.getAccount().getId() == null) {
            log.info("No account provided. Creating a default account for {}", user.getEmail());
            var account = accountService.createDefaultFor(user);
            user.setAccount(account);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        var saved = userRepository.save(user);
        log.info("User created with ID {}", saved.getId());
        return saved;
    }

    public List<User> findAll() {
        log.info("Fetching all users");
        var users = userRepository.findAll();
        log.info("Found {} users", users.size());
        return users;
    }

    @Transactional(readOnly = true)
    public List<User> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        log.info("Fetching users by ids {}", ids);
        var result = new ArrayList<>(userRepository.findAllById(ids));
        log.info("Found {} users for ids {}", result.size(), ids);
        return result;
    }

    public User findById(Long id) {
        log.info("Fetching user with ID {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with ID {}", id);
                    return new ResourceNotFoundException("User not found with ID " + id);
                });
    }

    public User update(Long id, User updated) {
        log.info("Updating user with ID {}", id);
        return userRepository.findById(id)
                .map(existing -> {
                    existing.setName(updated.getName());
                    existing.setEmail(updated.getEmail());
                    existing.setAccount(updated.getAccount());
                    var saved = userRepository.save(existing);
                    log.info("User with ID {} successfully updated", id);
                    return saved;
                })
                .orElseThrow(() -> {
                    log.warn("User not found with ID {}", id);
                    return new ResourceNotFoundException("User not found with ID " + id);
                });
    }

    public void delete(Long id) {
        log.info("Deleting user with ID {}", id);
        if (!userRepository.existsById(id)) {
            log.warn("User not found with ID {}", id);
            throw new ResourceNotFoundException("User not found with ID " + id);
        }
        userRepository.deleteById(id);
        log.info("User with ID {} successfully deleted", id);
    }

    public User findByEmail(String email) {
        log.info("Fetching user with email {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email {}", email);
                    return new ResourceNotFoundException("User not found with email " + email);
                });
    }
}