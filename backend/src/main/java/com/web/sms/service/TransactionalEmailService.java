package com.web.sms.service;

import com.web.sms.exception.ServiceUnavailableException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TransactionalEmailService {
    private static final Logger log = LoggerFactory.getLogger(TransactionalEmailService.class);

    private final JavaMailSender mailSender;
    private final RestClient restClient;
    private final String provider;
    private final String brevoApiKey;
    private final String mailUsername;
    private final String from;
    private final String fromName;

    public TransactionalEmailService(JavaMailSender mailSender,
                                     @Value("${btms.mail.provider:SMTP}") String provider,
                                     @Value("${btms.mail.brevo-api-url:https://api.brevo.com/v3/smtp/email}") String brevoApiUrl,
                                     @Value("${btms.mail.brevo-api-key:}") String brevoApiKey,
                                     @Value("${spring.mail.username:}") String mailUsername,
                                     @Value("${btms.mail.from:}") String from,
                                     @Value("${btms.mail.from-name:LBRCE BUS}") String fromName) {
        this.mailSender = mailSender;
        this.restClient = RestClient.create(brevoApiUrl);
        this.provider = provider == null ? "SMTP" : provider.trim().toUpperCase(Locale.ROOT);
        this.brevoApiKey = brevoApiKey;
        this.mailUsername = mailUsername;
        this.from = from;
        this.fromName = fromName;
    }

    public String sendHtml(String recipient, String subject, String html) {
        String sender = StringUtils.hasText(from) ? from : mailUsername;
        if (!StringUtils.hasText(sender)) throw new IllegalStateException("MAIL_FROM is not configured");
        try {
            if ("BREVO_API".equals(provider)) return sendWithBrevo(recipient, subject, html, sender);
            else if ("SMTP".equals(provider)) return sendWithSmtp(recipient, subject, html, sender);
            else throw new IllegalStateException("MAIL_PROVIDER must be BREVO_API or SMTP");
        } catch (ServiceUnavailableException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Transactional email failed through {} for recipient domain {}: {}",
                    provider, recipientDomain(recipient), exception.getMessage(), exception);
            throw new ServiceUnavailableException("Unable to send the email. Please try again shortly.", exception);
        }
    }

    private String sendWithBrevo(String recipient, String subject, String html, String sender) {
        if (!StringUtils.hasText(brevoApiKey)) throw new IllegalStateException("BREVO_API_KEY is not configured");
        BrevoResponse response = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("api-key", brevoApiKey)
                .body(Map.of(
                        "sender", Map.of("email", sender, "name", fromName),
                        "to", List.of(Map.of("email", recipient)),
                        "subject", subject,
                        "htmlContent", html))
                .retrieve()
                .body(BrevoResponse.class);
        if (response == null || !StringUtils.hasText(response.messageId())) {
            throw new IllegalStateException("Brevo returned an empty message ID");
        }
        return response.messageId();
    }

    private String sendWithSmtp(String recipient, String subject, String html, String sender) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
        helper.setFrom(new InternetAddress(sender, fromName, StandardCharsets.UTF_8.name()));
        helper.setTo(recipient);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
        return StringUtils.hasText(message.getMessageID()) ? message.getMessageID() : "smtp-accepted";
    }

    private String recipientDomain(String recipient) {
        int at = recipient == null ? -1 : recipient.lastIndexOf('@');
        return at >= 0 ? recipient.substring(at + 1) : "invalid";
    }

    private record BrevoResponse(String messageId) {}
}
