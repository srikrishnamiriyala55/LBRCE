package com.web.sms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_challenge", uniqueConstraints =
        @UniqueConstraint(name = "uk_otp_email_purpose", columnNames = {"email", "purpose"}))
public class OtpChallenge {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 254) private String email;
    @Column(nullable = false, length = 30) private String purpose;
    @Column(nullable = false, length = 64) private String otpHash;
    @Column(nullable = false, length = 32) private String salt;
    @Column(nullable = false) private LocalDateTime expiresAt;
    @Column(nullable = false) private LocalDateTime lastSentAt;
    @Column(nullable = false) private LocalDateTime windowStartedAt;
    @Column(nullable = false) private int sendCount;
    @Column(nullable = false) private int failedAttempts;
    private LocalDateTime consumedAt;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;

    @PrePersist void create(){LocalDateTime now=LocalDateTime.now();if(createdAt==null)createdAt=now;if(updatedAt==null)updatedAt=now;}
    @PreUpdate void update(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPurpose(){return purpose;} public void setPurpose(String v){purpose=v;}
    public String getOtpHash(){return otpHash;} public void setOtpHash(String v){otpHash=v;}
    public String getSalt(){return salt;} public void setSalt(String v){salt=v;}
    public LocalDateTime getExpiresAt(){return expiresAt;} public void setExpiresAt(LocalDateTime v){expiresAt=v;}
    public LocalDateTime getLastSentAt(){return lastSentAt;} public void setLastSentAt(LocalDateTime v){lastSentAt=v;}
    public LocalDateTime getWindowStartedAt(){return windowStartedAt;} public void setWindowStartedAt(LocalDateTime v){windowStartedAt=v;}
    public int getSendCount(){return sendCount;} public void setSendCount(int v){sendCount=v;}
    public int getFailedAttempts(){return failedAttempts;} public void setFailedAttempts(int v){failedAttempts=v;}
    public LocalDateTime getConsumedAt(){return consumedAt;} public void setConsumedAt(LocalDateTime v){consumedAt=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;}
}
