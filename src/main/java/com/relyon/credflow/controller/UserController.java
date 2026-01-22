package com.relyon.credflow.controller;

import static com.relyon.credflow.constant.BusinessConstants.Pagination.DEFAULT_PAGE_SIZE;

import com.relyon.credflow.model.mapper.UserMapper;
import com.relyon.credflow.model.user.*;
import com.relyon.credflow.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping
    public ResponseEntity<org.springframework.data.domain.Page<UserResponseDTO>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size) {
        log.info("GET request to fetch all users (page={}, size={})", page, size);
        var users = userService.findAll(page, size)
                .map(userMapper::toDto);
        log.info("Returning {} users", users.getTotalElements());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/select")
    public ResponseEntity<List<UserSelectDTO>> getAllSelect(
            @AuthenticationPrincipal AuthenticatedUser user) {

        log.info("GET select user list for account {}", user.getAccountId());
        var response = userService.findAllSelectByAccount(user.getAccountId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@securityService.canAccessUser(principal, #id)")
    public ResponseEntity<UserResponseDTO> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        log.info("GET request to fetch user with ID {}", id);
        var user = userService.findById(id);
        var response = userMapper.toDto(user);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityService.canAccessUser(principal, #id) and @securityService.canModify(principal)")
    public ResponseEntity<UserResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDTO dto,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        log.info("PUT request to update user with ID {}", id);
        User patch = userMapper.toEntity(dto);
        var saved = userService.update(id, patch);
        var response = userMapper.toDto(saved);
        log.info("User with ID {} successfully updated", id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@securityService.canAccessUser(principal, #id) and @securityService.canModify(principal)")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        log.info("DELETE request to remove user with ID {}", id);
        userService.delete(id);
        log.info("User with ID {} deleted", id);
        return ResponseEntity.noContent().build();
    }
}
