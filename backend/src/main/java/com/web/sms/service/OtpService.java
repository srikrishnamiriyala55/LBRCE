package com.web.sms.service;

import com.web.sms.dto.response.OtpChallengeResponse;
import com.web.sms.entity.OtpChallenge;
import com.web.sms.exception.BadRequestException;
import com.web.sms.repository.OtpChallengeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
public class OtpService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final OtpChallengeRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final int expiryMinutes;
    private final int maxAttempts;
    private final int resendCooldownSeconds;

    public OtpService(OtpChallengeRepository repository, PasswordEncoder passwordEncoder,
                      EmailService emailService,
                      @Value("${security.otp.expiry-minutes:5}") int expiryMinutes,
                      @Value("${security.otp.max-attempts:5}") int maxAttempts,
                      @Value("${security.otp.resend-cooldown-seconds:60}") int resendCooldownSeconds) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.expiryMinutes = expiryMinutes;
        this.maxAttempts = maxAttempts;
        this.resendCooldownSeconds = resendCooldownSeconds;
    }

    public OtpChallengeResponse create(String purpose, String principalId, String email) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("A valid email address must be configured for this account");
        }
        repository.findTopByPrincipalIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(principalId, purpose)
                .filter(existing -> existing.getLastSentAt().plusSeconds(resendCooldownSeconds).isAfter(LocalDateTime.now()))
                .ifPresent(existing -> { throw new BadRequestException("Please wait before requesting another OTP"); });
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        LocalDateTime now = LocalDateTime.now();
        OtpChallenge challenge = new OtpChallenge();
        challenge.setId(UUID.randomUUID().toString());
        challenge.setPurpose(purpose);
        challenge.setPrincipalId(principalId);
        challenge.setEmail(email.trim().toLowerCase());
        challenge.setOtpHash(passwordEncoder.encode(otp));
        challenge.setCreatedAt(now);
        challenge.setLastSentAt(now);
        challenge.setExpiresAt(now.plusMinutes(expiryMinutes));
        repository.save(challenge);
        try {
            emailService.sendOtp(challenge.getEmail(), otp,
                    purpose.startsWith("LOGIN_") ? "sign in" : "student registration", expiryMinutes);
        } catch (RuntimeException exception) {
            repository.deleteById(challenge.getId());
            throw exception;
        }
        return new OtpChallengeResponse(challenge.getId(), maskEmail(challenge.getEmail()), expiryMinutes * 60);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = BadRequestException.class)
    public OtpChallenge verify(String challengeId, String otp, Set<String> allowedPurposes) {
        OtpChallenge challenge = repository.findById(challengeId)
                .orElseThrow(() -> new BadRequestException("Verification request is invalid or expired"));
        if (challenge.isConsumed() || !allowedPurposes.contains(challenge.getPurpose())) {
            throw new BadRequestException("Verification request is invalid or already used");
        }
        if (challenge.getExpiresAt().isBefore(LocalDateTime.now())) {
            challenge.setConsumed(true);
            repository.save(challenge);
            throw new BadRequestException("OTP has expired. Please request a new code");
        }
        if (challenge.getFailedAttempts() >= maxAttempts) {
            challenge.setConsumed(true);
            repository.save(challenge);
            throw new BadRequestException("Too many incorrect attempts. Please request a new code");
        }
        if (!passwordEncoder.matches(otp, challenge.getOtpHash())) {
            challenge.setFailedAttempts(challenge.getFailedAttempts() + 1);
            if (challenge.getFailedAttempts() >= maxAttempts) challenge.setConsumed(true);
            repository.save(challenge);
            throw new BadRequestException("Incorrect OTP");
        }
        challenge.setConsumed(true);
        repository.save(challenge);
        return challenge;
    }

    public void delete(String challengeId) {
        if (challengeId != null) repository.deleteById(challengeId);
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***" + email.substring(Math.max(at, 0));
        return email.substring(0, 1) + "***" + email.substring(at - 1);
    }
}
