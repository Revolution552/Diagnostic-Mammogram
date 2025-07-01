package com.diagnostic.mammogram.service;

import com.diagnostic.mammogram.exception.EmailFailureException;
import com.diagnostic.mammogram.model.User;
import com.diagnostic.mammogram.model.VerificationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public EmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    private SimpleMailMessage createMailMessage() {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        return message;
    }

    public void sendVerificationEmail(VerificationToken verificationToken) throws EmailFailureException {
        User user = verificationToken.getUser();
        String toEmail = user.getEmail();
        String token = verificationToken.getToken();
        String verificationLink = "Verification Token is: " + token;

        SimpleMailMessage message = createMailMessage();
        message.setTo(toEmail);
        message.setSubject("Verify Your Email to Activate Your Account");
        message.setText("Hi " + user.getFullName() + ",\n\n"
                + "Please verify your email by Entering the following Token:\n"
                + verificationLink + "\n\n"
                + "This Token is valid for 1 hour.\n\nThank you!");

        logger.info("Attempting to send verification email to: {}", toEmail);
        logger.debug("Verification email content: \n{}", message.getText());

        try {
            javaMailSender.send(message);
            logger.info("Verification email successfully sent to: {}", toEmail);
        } catch (MailException e) {
            logger.error("Failed to send verification email to: {}", toEmail, e);
            throw new EmailFailureException("Failed to send verification email", e);
        }
    }

    public void sendPasswordResetEmail(User user, String token) throws EmailFailureException {
        String toEmail = user.getEmail();
        String resetLink = "Reset-password Token is: " + token;

        SimpleMailMessage message = createMailMessage();
        message.setTo(toEmail);
        message.setSubject("Reset Your Password");
        message.setText("Hi " + user.getFullName() + ",\n\n"
                + "You requested to reset your password. Enter the Token below to proceed:\n"
                + resetLink + "\n\n If you didn’t request this, please ignore this email.");

        logger.info("Attempting to send password reset email to: {}", toEmail);
        logger.debug("Password reset email content: \n{}", message.getText());

        try {
            javaMailSender.send(message);
            logger.info("Password reset email successfully sent to: {}", toEmail);
        } catch (MailException e) {
            logger.error("Failed to send password reset email to: {}", toEmail, e);
            throw new EmailFailureException("Failed to send password reset email", e);
        }
    }
}
