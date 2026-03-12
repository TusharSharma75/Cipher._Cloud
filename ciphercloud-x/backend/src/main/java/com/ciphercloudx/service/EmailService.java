package com.ciphercloudx.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@ciphercloudx.com}")
    private String fromEmail;

    @Value("${app.name:CipherCloud X}")
    private String appName;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Your " + appName + " Verification Code");
            message.setText(buildOtpEmailBody(otpCode));
            
            mailSender.send(message);
            log.debug("OTP email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
            // In development, log the OTP code
            log.info("DEV MODE - OTP Code for {}: {}", toEmail, otpCode);
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Password Reset Request - " + appName);
            message.setText(buildPasswordResetEmailBody(resetToken));
            
            mailSender.send(message);
            log.debug("Password reset email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
        }
    }

    @Async
    public void sendAccountLockedEmail(String toEmail, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Account Security Alert - " + appName);
            message.setText(buildAccountLockedEmailBody(username));
            
            mailSender.send(message);
            log.debug("Account locked email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send account locked email to: {}", toEmail, e);
        }
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Welcome to " + appName);
            message.setText(buildWelcomeEmailBody(username));
            
            mailSender.send(message);
            log.debug("Welcome email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", toEmail, e);
        }
    }

    private String buildOtpEmailBody(String otpCode) {
        return String.format(
            "Hello,%n%n" +
            "Your verification code for %s is: %s%n%n" +
            "This code will expire in 5 minutes.%n%n" +
            "If you didn't request this code, please ignore this email.%n%n" +
            "Best regards,%n" +
            "The %s Team",
            appName, otpCode, appName
        );
    }

    private String buildPasswordResetEmailBody(String resetToken) {
        return String.format(
            "Hello,%n%n" +
            "You have requested to reset your password for %s.%n%n" +
            "Your password reset token is: %s%n%n" +
            "This token will expire in 1 hour.%n%n" +
            "If you didn't request this, please ignore this email.%n%n" +
            "Best regards,%n" +
            "The %s Team",
            appName, resetToken, appName
        );
    }

    private String buildAccountLockedEmailBody(String username) {
        return String.format(
            "Hello %s,%n%n" +
            "Your %s account has been temporarily locked due to multiple failed login attempts.%n%n" +
            "For security reasons, your account will be automatically unlocked after 30 minutes.%n%n" +
            "If you didn't attempt to log in, please contact support immediately.%n%n" +
            "Best regards,%n" +
            "The %s Security Team",
            username, appName, appName
        );
    }

    private String buildWelcomeEmailBody(String username) {
        return String.format(
            "Welcome to %s, %s!%n%n" +
            "Your account has been successfully created.%n%n" +
            "You now have access to secure cloud storage with enterprise-grade encryption.%n%n" +
            "Key features:%n" +
            "- End-to-end encryption%n" +
            "- Multi-cloud redundancy%n" +
            "- File versioning%n" +
            "- Secure sharing%n%n" +
            "Get started by uploading your first file!%n%n" +
            "Best regards,%n" +
            "The %s Team",
            appName, username, appName
        );
    }
}
