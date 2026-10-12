package com.web.sms.service;

import com.web.sms.entity.OtpChallenge;
import com.web.sms.exception.BadRequestException;
import com.web.sms.exception.TooManyRequestsException;
import com.web.sms.repository.OtpChallengeRepository;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class OtpService {
    public static final String REGISTRATION = "REGISTRATION";
    private static final int EXPIRY_MINUTES = 5;
    private static final int MAX_SENDS_PER_WINDOW = 5;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpChallengeRepository repository;
    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String hashSecret;
    private final String mailUsername;
    private final String from;
    private final String fromName;

    public OtpService(OtpChallengeRepository repository,
                      JavaMailSender mailSender,
                      @Value("${otp.enabled:false}") boolean enabled,
                      @Value("${otp.hash-secret:}") String hashSecret,
                      @Value("${spring.mail.username:}") String mailUsername,
                      @Value("${btms.mail.from:}") String from,
                      @Value("${btms.mail.from-name:LBRCE BUS}") String fromName) {
        this.repository = repository;
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.hashSecret = hashSecret;
        this.mailUsername = mailUsername;
        this.from = from;
        this.fromName = fromName;
    }

    @Transactional
    public void send(String email, String purpose) {
        requireConfigured();
        String normalizedEmail = normalize(email);
        String normalizedPurpose = normalizePurpose(purpose);
        LocalDateTime now = LocalDateTime.now();
        OtpChallenge challenge = repository.findForUpdate(normalizedEmail, normalizedPurpose).orElseGet(OtpChallenge::new);

        if (challenge.getId() != null && challenge.getLastSentAt() != null
                && challenge.getLastSentAt().plusSeconds(30).isAfter(now)) {
            throw new TooManyRequestsException("Please wait 30 seconds before requesting another OTP");
        }
        boolean newWindow = challenge.getWindowStartedAt() == null || challenge.getWindowStartedAt().plusMinutes(15).isBefore(now);
        int nextCount = newWindow ? 1 : challenge.getSendCount() + 1;
        if (!newWindow && nextCount > MAX_SENDS_PER_WINDOW) {
            throw new TooManyRequestsException("OTP request limit reached. Try again after 15 minutes.");
        }

        String otp = "%06d".formatted(RANDOM.nextInt(1_000_000));
        byte[] saltBytes = new byte[16];
        RANDOM.nextBytes(saltBytes);
        String salt = HexFormat.of().formatHex(saltBytes);
        challenge.setEmail(normalizedEmail);
        challenge.setPurpose(normalizedPurpose);
        challenge.setSalt(salt);
        challenge.setOtpHash(hash(normalizedEmail, normalizedPurpose, otp, salt));
        challenge.setExpiresAt(now.plusMinutes(EXPIRY_MINUTES));
        challenge.setLastSentAt(now);
        challenge.setWindowStartedAt(newWindow ? now : challenge.getWindowStartedAt());
        challenge.setSendCount(nextCount);
        challenge.setFailedAttempts(0);
        challenge.setConsumedAt(null);
        challenge.setUpdatedAt(now);
        repository.saveAndFlush(challenge);
        sendEmail(normalizedEmail, otp, normalizedPurpose);
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public void verify(String email, String purpose, String otp) {
        requireConfigured();
        String normalizedEmail = normalize(email);
        String normalizedPurpose = normalizePurpose(purpose);
        if (otp == null || !otp.matches("^[0-9]{6}$")) throw new BadRequestException("OTP must contain 6 digits");
        OtpChallenge challenge = repository.findForUpdate(normalizedEmail, normalizedPurpose)
                .orElseThrow(() -> new BadRequestException("No OTP request was found. Request a new OTP."));
        LocalDateTime now = LocalDateTime.now();
        if (challenge.getConsumedAt() != null) throw new BadRequestException("This OTP has already been used. Request a new OTP.");
        if (challenge.getExpiresAt().isBefore(now)) throw new BadRequestException("OTP expired. Request a new OTP.");
        if (challenge.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) throw new BadRequestException("Too many incorrect attempts. Request a new OTP.");

        byte[] expected = challenge.getOtpHash().getBytes(StandardCharsets.US_ASCII);
        byte[] supplied = hash(normalizedEmail, normalizedPurpose, otp, challenge.getSalt()).getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expected, supplied)) {
            challenge.setFailedAttempts(challenge.getFailedAttempts() + 1);
            repository.save(challenge);
            throw new BadRequestException(challenge.getFailedAttempts() >= MAX_FAILED_ATTEMPTS
                    ? "Too many incorrect attempts. Request a new OTP." : "Invalid OTP");
        }
        challenge.setConsumedAt(now);
        repository.save(challenge);
    }

    private void sendEmail(String recipient, String otp, String purpose) {
        String sender = StringUtils.hasText(from) ? from : mailUsername;
        if (!StringUtils.hasText(sender)) throw new BadRequestException("OTP email sender is not configured");
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(sender, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(recipient);
            helper.setSubject("Your LBRCE BUS verification code");
            helper.setText("""
                    <!doctype html><html><body style="font-family:Arial,sans-serif;background:#f3f4f6;padding:24px">
                    <div style="max-width:560px;margin:auto;background:white;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb">
                      <div style="background:#1e40af;color:white;text-align:center;padding:20px"><img src="cid:lbrceLogo" width="64" height="64" style="background:white;border-radius:50%%;padding:4px"><h2 style="margin:8px 0 0">LBRCE BUS</h2></div>
                      <div style="padding:24px;text-align:center"><p>Use this code to complete your %s verification:</p><div style="font-size:32px;letter-spacing:8px;font-weight:bold;color:#1e3a8a;margin:22px">%s</div><p>This OTP is valid for 5 minutes and can be used only once.</p><p style="font-size:12px;color:#6b7280">If you did not request this code, do not share it with anyone.</p></div>
                    </div></body></html>
                    """.formatted(REGISTRATION.equals(purpose) ? "student registration" : "login", otp), true);
            ClassPathResource logo = new ClassPathResource("logo.jpg");
            if (logo.exists()) helper.addInline("lbrceLogo", logo, "image/jpeg");
            mailSender.send(message);
        } catch (Exception exception) {
            throw new BadRequestException("Unable to send the OTP email. Please try again shortly.");
        }
    }

    private String hash(String email, String purpose, String otp, String salt) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hashSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((salt + "|" + email + "|" + purpose + "|" + otp).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) { throw new IllegalStateException("Unable to secure OTP", exception); }
    }

    private void requireConfigured() {
        if (!enabled) throw new BadRequestException("Email OTP is not enabled. Contact the transport administrator.");
        if (!StringUtils.hasText(hashSecret) || hashSecret.length() < 32) throw new IllegalStateException("OTP_HASH_SECRET must contain at least 32 characters");
    }
    private String normalize(String email) {
        if (email == null || email.isBlank()) throw new BadRequestException("Email is required");
        return email.trim().toLowerCase();
    }
    private String normalizePurpose(String purpose) {
        if (!(purpose != null && purpose.startsWith("LOGIN_") && purpose.length() == 30) && !REGISTRATION.equals(purpose)) throw new IllegalArgumentException("Invalid OTP purpose");
        return purpose;
    }

    public static String loginPurpose(String loginId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(loginId.trim().toUpperCase().getBytes(StandardCharsets.UTF_8));
            return "LOGIN_" + HexFormat.of().formatHex(digest).substring(0, 24);
        } catch (Exception exception) { throw new IllegalStateException("Unable to create OTP login context", exception); }
    }
}
