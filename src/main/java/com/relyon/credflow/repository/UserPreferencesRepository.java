package com.relyon.credflow.repository;

import com.relyon.credflow.model.user.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {

    Optional<UserPreferences> findByUserId(Long userId);

    Optional<UserPreferences> findByUserIdAndUserAccountId(Long userId, Long accountId);
}
