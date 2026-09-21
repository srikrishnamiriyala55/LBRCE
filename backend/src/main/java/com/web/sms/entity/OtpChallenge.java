package com.web.sms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_challenge", indexes = {
        @Index(name = "idx_otp_challenge_principal", columnList = "principalId,purpose"),
        @Index(name = "idx_otp_challenge_expires", columnList = "expiresAt")
})
public class OtpChallenge {
    @Id
    @Column(length = 36)
    private String id;
    @Column(nullable = false, length = 30)
    private String purpose;
    @Column(nullable = false, length = 120)
    private String principalId;
    @Column(nullable = false, length = 120)
    private String email;
    @Column(nullable = false, length = 100)
    private String otpHash;
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    @Column(nullable = false)
    private LocalDateTime lastSentAt;
    @Column(nullable = false)
    private int failedAttempts;
    @Column(nullable = false)
    private boolean consumed;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getPrincipalId() { return principalId; }
    public void setPrincipalId(String principalId) { this.principalId = principalId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getLastSentAt() { return lastSentAt; }
    public void setLastSentAt(LocalDateTime lastSentAt) { this.lastSentAt = lastSentAt; }
    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }
    public boolean isConsumed() { return consumed; }
    public void setConsumed(boolean consumed) { this.consumed = consumed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
