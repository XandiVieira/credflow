package com.relyon.credflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService();
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@credflow.test");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:3000");
    }

    @Nested
    class SendPasswordResetEmail {

        @Test
        void whenMailSenderConfigured_shouldSendEmail() {
            ReflectionTestUtils.setField(emailService, "mailSender", mailSender);

            emailService.sendPasswordResetEmail("user@test.com", "reset-token-123");

            var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());

            var message = captor.getValue();
            assertThat(message.getFrom()).isEqualTo("noreply@credflow.test");
            assertThat(message.getTo()).containsExactly("user@test.com");
            assertThat(message.getSubject()).isEqualTo("Password Reset Request");
            assertThat(message.getText()).contains("http://localhost:3000/reset-password?token=reset-token-123");
        }

        @Test
        void whenMailSenderNotConfigured_shouldNotThrow() {
            ReflectionTestUtils.setField(emailService, "mailSender", null);

            emailService.sendPasswordResetEmail("user@test.com", "token-abc");

            verifyNoInteractions(mailSender);
        }

        @Test
        void whenMailSenderThrows_shouldPropagateException() {
            ReflectionTestUtils.setField(emailService, "mailSender", mailSender);
            doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

            assertThatThrownBy(() -> emailService.sendPasswordResetEmail("user@test.com", "token"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("email.sendFailed");
        }

        @Test
        void emailBodyContainsExpiredWarning() {
            ReflectionTestUtils.setField(emailService, "mailSender", mailSender);

            emailService.sendPasswordResetEmail("user@test.com", "token");

            var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());

            assertThat(captor.getValue().getText()).contains("expire in 1 hour");
        }
    }

    @Nested
    class SendAccountInvitationEmail {

        @Test
        void whenMailSenderConfigured_shouldSendEmailWithInviterName() {
            ReflectionTestUtils.setField(emailService, "mailSender", mailSender);

            emailService.sendAccountInvitationEmail("invitee@test.com", "INV123", "John Doe");

            var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
            verify(mailSender).send(captor.capture());

            var message = captor.getValue();
            assertThat(message.getTo()).containsExactly("invitee@test.com");
            assertThat(message.getSubject()).contains("invited to join");
            assertThat(message.getText()).contains("John Doe");
            assertThat(message.getText()).contains("INV123");
            assertThat(message.getText()).contains("http://localhost:3000/join-account?code=INV123");
        }

        @Test
        void whenMailSenderNotConfigured_shouldNotThrow() {
            ReflectionTestUtils.setField(emailService, "mailSender", null);

            emailService.sendAccountInvitationEmail("invitee@test.com", "CODE", "Inviter");

            verifyNoInteractions(mailSender);
        }

        @Test
        void whenMailSenderThrows_shouldPropagateException() {
            ReflectionTestUtils.setField(emailService, "mailSender", mailSender);
            doThrow(new RuntimeException("Connection refused")).when(mailSender).send(any(SimpleMailMessage.class));

            assertThatThrownBy(() -> emailService.sendAccountInvitationEmail("test@test.com", "CODE", "Name"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("email.sendFailed");
        }
    }
}
