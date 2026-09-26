package com.web.sms.service;

import com.web.sms.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {
    private record Attempt(int failures, Instant blockedUntil) {}

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration blockDuration;

    public LoginAttemptService(@Value("${security.login.max-failures}") int maxFailures,
                               @Value("${security.login.block-minutes}") long blockMinutes) {
        if (maxFailures < 3 || blockMinutes < 1) {
            throw new IllegalArgumentException("Login protection settings are too low");
        }
        this.maxFailures = maxFailures;
        this.blockDuration = Duration.ofMinutes(blockMinutes);
    }

    public void verifyAllowed(String loginId, String remoteAddress) {
        String key = key(loginId, remoteAddress);
        Attempt attempt = attempts.get(key);
        if (attempt == null) return;
        if (attempt.blockedUntil() != null && attempt.blockedUntil().isAfter(Instant.now())) {
            throw new TooManyRequestsException("Too many failed sign-in attempts. Try again later.");
        }
        if (attempt.blockedUntil() != null) attempts.remove(key, attempt);
    }

    public void recordFailure(String loginId, String remoteAddress) {
        String key = key(loginId, remoteAddress);
        attempts.compute(key, (ignored, current) -> {
            int failures = current == null ? 1 : current.failures() + 1;
            return failures >= maxFailures
                    ? new Attempt(failures, Instant.now().plus(blockDuration))
                    : new Attempt(failures, null);
        });
    }

    public void recordSuccess(String loginId, String remoteAddress) {
        attempts.remove(key(loginId, remoteAddress));
    }

    private String key(String loginId, String remoteAddress) {
        String normalizedId = loginId == null ? "" : loginId.trim().toUpperCase(Locale.ROOT);
        return normalizedId + '|' + (remoteAddress == null ? "unknown" : remoteAddress);
    }
}
