package com.relyon.credflow.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Email service with conditional SMTP support.
 * <p>
 * In development (when JavaMailSender is not configured), emails are logged only.
 * In production, set up SMTP by adding to application.yaml:
 * <pre>
 * spring:
 *   mail:
 *     host: smtp.gmail.com
 *     port: 587
 *     username: ${EMAIL_USERNAME}
 *     password: ${EMAIL_APP_PASSWORD}
 *     properties:
 *       mail.smtp.auth: true
 *       mail.smtp.starttls.enable: true
 * app:
 *   mail:
 *     from: noreply@credflow.com
 *   frontend:
 *     url: https://yourapp.com
 * </pre>
 */
@Service
@Slf4j
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@credflow.local}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public void sendPasswordResetEmail(String toEmail, String token) {
        var emailBody = buildPasswordResetEmailBody(token);

        if (mailSender == null) {
            logMockEmail(toEmail, "Password Reset Request", emailBody, token);
            return;
        }

        log.info("Sending password reset email to: {}", toEmail);
        var message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Password Reset Request");
        message.setText(emailBody);

        try {
            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("email.sendFailed", e);
        }
    }

    public void sendAccountInvitationEmail(String toEmail, String inviteCode, String inviterName) {
        var emailBody = buildAccountInvitationEmailBody(inviteCode, inviterName);

        if (mailSender == null) {
            logMockEmail(toEmail, "Account Invitation", emailBody, inviteCode);
            return;
        }

        log.info("Sending account invitation email to: {}", toEmail);
        var message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("You've been invited to join an account on CredFlow");
        message.setText(emailBody);

        try {
            mailSender.send(message);
            log.info("Account invitation email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send account invitation email to: {}", toEmail, e);
            throw new RuntimeException("email.sendFailed", e);
        }
    }

    private void logMockEmail(String toEmail, String subject, String body, String token) {
        log.info("================================================================================");
        log.info("MOCK EMAIL (JavaMailSender not configured)");
        log.info("================================================================================");
        log.info("To: {}", toEmail);
        log.info("From: {}", fromEmail);
        log.info("Subject: {}", subject);
        log.info("");
        log.info("Body:");
        log.info("{}", body);
        log.info("");
        log.info("Token/Code (for testing): {}", token);
        log.info("================================================================================");
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
