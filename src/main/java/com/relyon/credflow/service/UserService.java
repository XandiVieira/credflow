package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceAlreadyExistsException;
import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.model.user.UserRequestDTO;
import com.relyon.credflow.model.user.UserSelectDTO;
import com.relyon.credflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final LocalizedMessageTranslationService translationService;

    @Transactional
    public User create(UserRequestDTO dto) {
        log.info("Creating user: {}", dto.getEmail());

        var user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setName(dto.getName());

        if (dto.getInviteCode() != null && !dto.getInviteCode().isBlank()) {
            user.setAccount(accountService.findByInviteCode(dto.getInviteCode()));
        } else {
            user.setAccount(accountService.createDefaultFor(user));
        }

        try {
            User saved = userRepository.save(user);
            log.info("User created with ID {} and account ID {}", saved.getId(), saved.getAccount().getId());
            return saved;
        } catch (DataIntegrityViolationException ex) {
            log.warn("Email {} already exists", dto.getEmail());
            throw new ResourceAlreadyExistsException("user.emailAlreadyExists", dto.getEmail());
        }
    }

    public Page<User> findAll(int page, int size) {
        log.info("Fetching users (page={}, size={})", page, size);
        var pageable = PageRequest.of(page, size);
        var users = userRepository.findAll(pageable);
        log.info("Found {} users", users.getTotalElements());
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
                    return new ResourceNotFoundException("resource.user.notFound", id);
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
                    return new ResourceNotFoundException("resource.user.notFound", id);
                });
    }

    public void delete(Long id) {
        log.info("Deleting user with ID {}", id);
        if (!userRepository.existsById(id)) {
            log.warn("User not found with ID {}", id);
            throw new ResourceNotFoundException("resource.user.notFound", id);
        }
        userRepository.deleteById(id);
        log.info("User with ID {} successfully deleted", id);
    }

    public User findByEmail(String email) {
        log.info("Fetching user with email {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email {}", email);
                    return new ResourceNotFoundException("resource.user.notFoundByEmail", email);
                });
    }

    /**
     * Returns a simple list of users with only id and first name (for dropdowns/selects)
     */
    public List<UserSelectDTO> findAllSelectByAccount(Long accountId) {
        log.info("Fetching select user list for account {}", accountId);
        return userRepository.findByAccountId(accountId).stream()
                .map(user -> {
                    String firstName = extractFirstName(user.getName());
                    return new UserSelectDTO(user.getId(), firstName);
                })
                .toList();
    }

    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts[0];
    }
}