package com.relyon.credflow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public void sendPasswordResetEmail(String toEmail, String token) {
        log.info("Sending password reset email to: {}", toEmail);

        var message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Password Reset Request");
        message.setText(buildPasswordResetEmailBody(token));

        try {
            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("email.sendFailed", e);
        }
    }

    public void sendAccountInvitationEmail(String toEmail, String inviteCode, String inviterName) {
        log.info("Sending account invitation email to: {}", toEmail);

        var message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("You've been invited to join an account on CredFlow");
        message.setText(buildAccountInvitationEmailBody(inviteCode, inviterName));

        try {
            mailSender.send(message);
            log.info("Account invitation email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send account invitation email to: {}", toEmail, e);
            throw new RuntimeException("email.sendFailed", e);
        }
    }

    private String buildPasswordResetEmailBody(String token) {
        var resetUrl = frontendUrl + "/reset-password?token=" + token;
        return """
                Hello,

                You requested to reset your password. Please click the link below to reset your password:

                %s

                This link will expire in 1 hour.

                If you did not request this password reset, please ignore this email.

                Best regards,
                CredFlow Team
                """.formatted(resetUrl);
    }

    private String buildAccountInvitationEmailBody(String inviteCode, String inviterName) {
        var inviteUrl = frontendUrl + "/join-account?code=" + inviteCode;
        return """
                Hello,

                %s has invited you to join their account on CredFlow.

                To accept this invitation, please use the following invite code:

                Invite Code: %s

                Or click the link below to join automatically:

                %s

                Best regards,
                CredFlow Team
                """.formatted(inviterName, inviteCode, inviteUrl);
    }
}
