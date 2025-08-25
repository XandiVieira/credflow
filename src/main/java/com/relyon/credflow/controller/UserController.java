package com.relyon.credflow.controller;

import com.relyon.credflow.model.mapper.UserMapper;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.model.user.UserRequestDTO;
import com.relyon.credflow.model.user.UserResponseDTO;
import com.relyon.credflow.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserMapper mapper;

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> findAll() {
        log.info("GET request to fetch all users");
        var users = userService.findAll().stream()
                .map(mapper::toDto)
                .toList();
        log.info("Returning {} users", users.size());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> findById(@PathVariable Long id) {
        log.info("GET request to fetch user with ID {}", id);
        var user = userService.findById(id);
        var response = mapper.toDto(user);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDTO dto
    ) {
        log.info("PUT request to update user with ID {}", id);
        User patch = mapper.toEntity(dto);
        var saved = userService.update(id, patch);
        var response = mapper.toDto(saved);
        log.info("User with ID {} successfully updated", id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE request to remove user with ID {}", id);
        userService.delete(id);
        log.info("User with ID {} deleted", id);
        return ResponseEntity.noContent().build();
    }
}