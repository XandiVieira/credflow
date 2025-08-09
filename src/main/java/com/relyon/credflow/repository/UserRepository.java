package com.relyon.credflow.repository;

import com.relyon.credflow.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByAccountId(Long accountId);

    Optional<User> findByEmail(String email);
}