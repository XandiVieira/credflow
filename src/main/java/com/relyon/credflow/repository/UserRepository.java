package com.relyon.credflow.repository;

import com.relyon.credflow.model.user.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByAccountId(Long accountId);
    Optional<User> findByEmail(String email);
}