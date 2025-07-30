package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.AccountRepository;
import com.relyon.credflow.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID " + id));
    }

    public User create(User user) {
        log.info("Creating user: {}", user);
        return userRepository.save(user);
    }

    public User update(Long id, User updated) {
        return userRepository.findById(id).map(existing -> {
            existing.setName(updated.getName());
            existing.setEmail(updated.getEmail());
            existing.setAccount(updated.getAccount());
            return userRepository.save(existing);
        }).orElseThrow(() -> new ResourceNotFoundException("User not found with ID " + id));
    }

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with ID " + id);
        }
        userRepository.deleteById(id);
    }
}