package com.web.sms.service;

import com.web.sms.entity.OtpChallenge;
import com.web.sms.exception.BadRequestException;
import com.web.sms.repository.OtpChallengeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {
    @Mock OtpChallengeRepository repository;
    @Mock TransactionalEmailService emailService;
    OtpService service;

    @BeforeEach
    void setUp() {
        service = new OtpService(repository, emailService,
                new OfficialBrandingService("https://btms.example.edu"), true,
                "test-only-otp-hash-secret-with-more-than-32-characters");
    }

    @Test
    void sendsPurposeBoundOtpStoresOnlyHashAndVerifiesOnce() {
        when(repository.findForUpdate("student@example.com", OtpService.REGISTRATION))
                .thenReturn(Optional.empty());
        when(emailService.sendHtml(eq("student@example.com"), anyString(), anyString()))
                .thenReturn("brevo-message-1");

        service.send(" Student@Example.com ", OtpService.REGISTRATION);

        ArgumentCaptor<OtpChallenge> challengeCaptor = ArgumentCaptor.forClass(OtpChallenge.class);
        verify(repository).saveAndFlush(challengeCaptor.capture());
        OtpChallenge challenge = challengeCaptor.getValue();
        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendHtml(eq("student@example.com"), anyString(), htmlCaptor.capture());
        String otp = extractOtp(htmlCaptor.getValue());
        assertTrue(htmlCaptor.getValue().contains("https://btms.example.edu/logo.jpg"));
        assertTrue(htmlCaptor.getValue().contains("Lakireddy Bali Reddy College of Engineering"));
        assertNotEquals(otp, challenge.getOtpHash());
        assertEquals("SENT", challenge.getDeliveryStatus());
        assertEquals("brevo-message-1", challenge.getProviderMessageId());

        when(repository.findForUpdate("student@example.com", OtpService.REGISTRATION))
                .thenReturn(Optional.of(challenge));
        service.verify("student@example.com", OtpService.REGISTRATION, otp);
        assertNotNull(challenge.getConsumedAt());

        BadRequestException reused = assertThrows(BadRequestException.class,
                () -> service.verify("student@example.com", OtpService.REGISTRATION, otp));
        assertTrue(reused.getMessage().contains("already been used"));
    }

    @Test
    void incrementsFailedAttemptsForIncorrectOtp() {
        when(repository.findForUpdate("student@example.com", OtpService.REGISTRATION))
                .thenReturn(Optional.empty());
        when(emailService.sendHtml(anyString(), anyString(), anyString())).thenReturn("message-2");
        service.send("student@example.com", OtpService.REGISTRATION);

        ArgumentCaptor<OtpChallenge> captor = ArgumentCaptor.forClass(OtpChallenge.class);
        verify(repository).saveAndFlush(captor.capture());
        OtpChallenge challenge = captor.getValue();
        when(repository.findForUpdate("student@example.com", OtpService.REGISTRATION))
                .thenReturn(Optional.of(challenge));

        assertThrows(BadRequestException.class,
                () -> service.verify("student@example.com", OtpService.REGISTRATION, "000000"));
        assertEquals(1, challenge.getFailedAttempts());
    }

    private String extractOtp(String html) {
        Matcher matcher = Pattern.compile(">(\\d{6})</div>").matcher(html);
        assertTrue(matcher.find(), "OTP must be present in the outbound email HTML");
        return matcher.group(1);
    }
}
