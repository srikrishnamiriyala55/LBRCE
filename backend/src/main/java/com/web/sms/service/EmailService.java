package com.web.sms.service;

import com.web.sms.exception.BadRequestException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String fromAddress;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                        @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSenderProvider = mailSenderProvider;
        this.fromAddress = fromAddress;
    }

    public void sendOtp(String recipient, String otp, String purpose, int expiryMinutes) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null || fromAddress.isBlank()) {
            throw new BadRequestException("Email verification is temporarily unavailable. Contact the administrator");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject("LBRCE BTMS verification code");
        message.setText("Your verification code for " + purpose + " is " + otp
                + ". It expires in " + expiryMinutes + " minutes. Do not share this code with anyone.");
        try {
            sender.send(message);
        } catch (RuntimeException exception) {
            log.error("Unable to send OTP email through the configured SMTP service", exception);
            throw new BadRequestException("Unable to send the verification email. Please try again later");
        }
    }
}
