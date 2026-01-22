package com.relyon.credflow.repository;

import com.relyon.credflow.model.user.UserPreferences;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {

    Optional<UserPreferences> findByUserId(Long userId);

    Optional<UserPreferences> findByUserIdAndUserAccountId(Long userId, Long accountId);
}
