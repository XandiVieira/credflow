package com.relyon.credflow.repository;

import com.relyon.credflow.model.user.User;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByAccountId(Long accountId);

    Optional<User> findByEmail(String email);

    Optional<User> findByIdAndAccountId(Long id, Long accountId);

    Optional<User> findByPasswordResetToken(String token);

    List<User> findAllByIdInAndAccountId(Set<Long> ids, Long accountId);

    boolean existsByIdAndAccountId(Long userId, Long accountId);
}