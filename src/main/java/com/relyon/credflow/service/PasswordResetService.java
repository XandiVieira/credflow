package com.relyon.credflow.service;

import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final int TOKEN_EXPIRY_HOURS = 1;

    @Transactional
    public void initiatePasswordReset(String email) {
        log.info("Initiating password reset for email: {}", email);

        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("resource.user.notFoundByEmail", email));

        var token = generateResetToken();
        user.setPasswordResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS));
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), token);

        log.info("Password reset token generated for user {}", user.getId());
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        log.info("Resetting password with token");

        var user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("auth.invalidResetToken"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("auth.resetTokenExpired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        log.info("Password reset successfully for user {}", user.getId());
    }

    @Transactional
    public void validateResetToken(String token) {
        var user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("auth.invalidResetToken"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("auth.resetTokenExpired");
        }
    }

    private String generateResetToken() {
        return UUID.randomUUID().toString();
    }
}
