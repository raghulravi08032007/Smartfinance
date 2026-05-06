package com.rawgul.service;

import com.rawgul.model.User;
import com.rawgul.model.SupportTicket;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for sending email notifications
 * Supports both plain text and HTML template-based emails
 * All email sending operations are asynchronous using @Async
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.fromName}")
    private String fromName;

    @Value("${app.email.support}")
    private String supportEmail;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    /**
     * Send welcome email to newly registered users
     * @param user The newly registered user
     */
    @Async
    public void sendWelcomeEmail(User user) {
        try {
            log.info("Sending welcome email to: {}", user.getEmail());

            Context context = new Context();
            context.setVariable("username", user.getUsername());
            context.setVariable("email", user.getEmail());
            context.setVariable("registrationDate", LocalDateTime.now().format(DATE_FORMATTER));
            context.setVariable("supportEmail", supportEmail);

            String htmlContent = templateEngine.process("email/welcome-email", context);

            sendHtmlEmail(
                user.getEmail(),
                "Welcome to Finance Tracker!",
                htmlContent
            );

            log.info("Welcome email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", user.getEmail(), e);
            // Fallback to plain text email
            sendWelcomeEmailPlainText(user);
        }
    }

    /**
     * Fallback method to send plain text welcome email
     */
    private void sendWelcomeEmailPlainText(User user) {
        try {
            String content = String.format(
                "Hi %s,\n\n" +
                "Welcome to Finance Tracker!\n\n" +
                "Your account has been successfully created. You can now start tracking your finances, " +
                "managing your budget, and achieving your financial goals.\n\n" +
                "Account Details:\n" +
                "- Username: %s\n" +
                "- Email: %s\n" +
                "- Registration Date: %s\n\n" +
                "If you have any questions, please don't hesitate to contact our support team at %s\n\n" +
                "Best regards,\n" +
                "Finance Tracker Team",
                user.getUsername(),
                user.getUsername(),
                user.getEmail(),
                LocalDateTime.now().format(DATE_FORMATTER),
                supportEmail
            );

            sendPlainTextEmail(user.getEmail(), "Welcome to Finance Tracker!", content);
            log.info("Plain text welcome email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send plain text welcome email to: {}", user.getEmail(), e);
        }
    }

    /**
     * Send password reset email with reset token
     * @param email User's email address
     * @param username User's username
     * @param resetToken Password reset token
     * @param expirationHours Token expiration time in hours
     */
    @Async
    public void sendPasswordResetEmail(String email, String username, String resetToken, int expirationHours) {
        try {
            log.info("Sending password reset email to: {}", email);

            // In production, this should be your actual frontend URL
            String resetUrl = "http://localhost:8080/forgot-password.html?token=" + resetToken;

            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("resetUrl", resetUrl);
            context.setVariable("expirationHours", expirationHours);
            context.setVariable("supportEmail", supportEmail);

            String htmlContent = templateEngine.process("email/password-reset-email", context);

            sendHtmlEmail(
                email,
                "Password Reset Request - Finance Tracker",
                htmlContent
            );

            log.info("Password reset email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", email, e);
            // Fallback to plain text email
            sendPasswordResetEmailPlainText(email, username, resetToken, expirationHours);
        }
    }

    /**
     * Fallback method to send plain text password reset email
     */
    private void sendPasswordResetEmailPlainText(String email, String username, String resetToken, int expirationHours) {
        try {
            String resetUrl = "http://localhost:8080/forgot-password.html?token=" + resetToken;
            
            String content = String.format(
                "Hi %s,\n\n" +
                "We received a request to reset your password for your Finance Tracker account.\n\n" +
                "To reset your password, please click the link below:\n" +
                "%s\n\n" +
                "This link will expire in %d hours.\n\n" +
                "If you didn't request a password reset, please ignore this email or contact support at %s\n\n" +
                "For security reasons, never share this link with anyone.\n\n" +
                "Best regards,\n" +
                "Finance Tracker Team",
                username,
                resetUrl,
                expirationHours,
                supportEmail
            );

            sendPlainTextEmail(email, "Password Reset Request - Finance Tracker", content);
            log.info("Plain text password reset email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send plain text password reset email to: {}", email, e);
        }
    }

    /**
     * Send support ticket confirmation email
     * @param ticket The support ticket
     */
    @Async
    public void sendSupportTicketConfirmation(SupportTicket ticket) {
        try {
            log.info("Sending support ticket confirmation email to: {}", ticket.getContactEmail());

            Context context = new Context();
            context.setVariable("name", ticket.getFullName());
            context.setVariable("ticketNumber", ticket.getTicketNumber());
            context.setVariable("subject", ticket.getSubject());
            context.setVariable("message", ticket.getMessage());
            context.setVariable("status", ticket.getStatus().name());
            context.setVariable("priority", ticket.getPriority().name());
            context.setVariable("createdDate", ticket.getCreatedDate().format(DATE_FORMATTER));
            context.setVariable("supportEmail", supportEmail);

            String htmlContent = templateEngine.process("email/support-ticket-confirmation", context);

            sendHtmlEmail(
                ticket.getContactEmail(),
                "Support Ticket #" + ticket.getTicketNumber() + " - Received",
                htmlContent
            );

            log.info("Support ticket confirmation email sent successfully to: {}", ticket.getContactEmail());
        } catch (Exception e) {
            log.error("Failed to send support ticket confirmation email to: {}", ticket.getContactEmail(), e);
            // Fallback to plain text email
            sendSupportTicketConfirmationPlainText(ticket);
        }
    }

    /**
     * Fallback method to send plain text support ticket confirmation email
     */
    private void sendSupportTicketConfirmationPlainText(SupportTicket ticket) {
        try {
            String content = String.format(
                "Hi %s,\n\n" +
                "Thank you for contacting Finance Tracker support. We have received your support ticket.\n\n" +
                "Ticket Details:\n" +
                "- Ticket Number: %s\n" +
                "- Subject: %s\n" +
                "- Status: %s\n" +
                "- Priority: %s\n" +
                "- Created: %s\n\n" +
                "Your Message:\n" +
                "%s\n\n" +
                "Our support team will review your ticket and respond as soon as possible. " +
                "You can track the status of your ticket by logging into your dashboard.\n\n" +
                "If you have any additional information to add, please reply to this email " +
                "or contact us at %s\n\n" +
                "Best regards,\n" +
                "Finance Tracker Support Team",
                ticket.getFullName(),
                ticket.getTicketNumber(),
                ticket.getSubject(),
                ticket.getStatus().name(),
                ticket.getPriority().name(),
                ticket.getCreatedDate().format(DATE_FORMATTER),
                ticket.getMessage(),
                supportEmail
            );

            sendPlainTextEmail(ticket.getContactEmail(), "Support Ticket #" + ticket.getTicketNumber() + " - Received", content);
            log.info("Plain text support ticket confirmation email sent to: {}", ticket.getContactEmail());
        } catch (Exception e) {
            log.error("Failed to send plain text support ticket confirmation email to: {}", ticket.getContactEmail(), e);
        }
    }

    /**
     * Send support ticket status update email
     * @param ticket The support ticket with updated status
     * @param notes Optional notes about the status change
     */
    @Async
    public void sendTicketStatusUpdate(SupportTicket ticket, String notes) {
        try {
            log.info("Sending ticket status update email to: {}", ticket.getContactEmail());

            Context context = new Context();
            context.setVariable("name", ticket.getFullName());
            context.setVariable("ticketNumber", ticket.getTicketNumber());
            context.setVariable("subject", ticket.getSubject());
            context.setVariable("status", ticket.getStatus().name());
            context.setVariable("updatedDate", LocalDateTime.now().format(DATE_FORMATTER));
            context.setVariable("notes", notes != null ? notes : "No additional notes");
            context.setVariable("supportEmail", supportEmail);

            String htmlContent = templateEngine.process("email/ticket-status-update", context);

            sendHtmlEmail(
                ticket.getContactEmail(),
                "Support Ticket #" + ticket.getTicketNumber() + " - Status Updated",
                htmlContent
            );

            log.info("Ticket status update email sent successfully to: {}", ticket.getContactEmail());
        } catch (Exception e) {
            log.error("Failed to send ticket status update email to: {}", ticket.getContactEmail(), e);
        }
    }

    /**
     * Send HTML email using Thymeleaf template
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send HTML email to: {}", to, e);
            throw new MessagingException("Failed to send email", e);
        }
    }

    /**
     * Send plain text email
     */
    private void sendPlainTextEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);

        mailSender.send(message);
    }

    /**
     * Send generic notification email
     * @param to Recipient email address
     * @param subject Email subject
     * @param message Email message content
     */
    @Async
    public void sendNotificationEmail(String to, String subject, String message) {
        try {
            log.info("Sending notification email to: {}", to);
            sendPlainTextEmail(to, subject, message);
            log.info("Notification email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send notification email to: {}", to, e);
        }
    }
}
