package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    public List<User> findAll() {
        log.info("Fetching all users");
        var users = userRepository.findAll();
        log.info("Found {} users", users.size());
        return users;
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

    public User create(User user) {
        log.info("Creating user: {}", user);
        return userRepository.save(user);
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