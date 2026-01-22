package com.relyon.credflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.relyon.credflow.exception.ResourceNotFoundException;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encodedPassword")
                .build();
    }

    @Test
    void initiatePasswordReset_validEmail_shouldGenerateTokenAndSendEmail() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        passwordResetService.initiatePasswordReset("test@example.com");

        var userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        var savedUser = userCaptor.getValue();
        assertThat(savedUser.getPasswordResetToken()).isNotNull();
        assertThat(savedUser.getResetTokenExpiry()).isAfter(LocalDateTime.now());

        verify(emailService).sendPasswordResetEmail(eq("test@example.com"), anyString());
    }

    @Test
    void initiatePasswordReset_invalidEmail_shouldThrowException() {
        when(userRepository.findByEmail("invalid@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.initiatePasswordReset("invalid@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void resetPassword_validToken_shouldResetPassword() {
        var token = "valid-token";
        user.setPasswordResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));

        when(userRepository.findByPasswordResetToken(token)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        passwordResetService.resetPassword(token, "newPassword");

        var userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        var savedUser = userCaptor.getValue();
        assertThat(savedUser.getPassword()).isEqualTo("encodedNewPassword");
        assertThat(savedUser.getPasswordResetToken()).isNull();
        assertThat(savedUser.getResetTokenExpiry()).isNull();
    }

    @Test
    void resetPassword_invalidToken_shouldThrowException() {
        when(userRepository.findByPasswordResetToken("invalid-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword("invalid-token", "newPassword"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("auth.invalidResetToken");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_expiredToken_shouldThrowException() {
        var token = "expired-token";
        user.setPasswordResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().minusHours(1));

        when(userRepository.findByPasswordResetToken(token)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> passwordResetService.resetPassword(token, "newPassword"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("auth.resetTokenExpired");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void validateResetToken_validToken_shouldNotThrow() {
        var token = "valid-token";
        user.setPasswordResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));

        when(userRepository.findByPasswordResetToken(token)).thenReturn(Optional.of(user));

        passwordResetService.validateResetToken(token);

        verify(userRepository).findByPasswordResetToken(token);
    }

    @Test
    void validateResetToken_invalidToken_shouldThrowException() {
        when(userRepository.findByPasswordResetToken("invalid-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.validateResetToken("invalid-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("auth.invalidResetToken");
    }

    @Test
    void validateResetToken_expiredToken_shouldThrowException() {
        var token = "expired-token";
        user.setPasswordResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().minusHours(1));

        when(userRepository.findByPasswordResetToken(token)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> passwordResetService.validateResetToken(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("auth.resetTokenExpired");
    }
}
