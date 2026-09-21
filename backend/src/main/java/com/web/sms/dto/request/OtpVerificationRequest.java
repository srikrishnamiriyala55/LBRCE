package com.web.sms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class OtpVerificationRequest {
    @NotBlank(message = "Verification request is missing")
    private String challengeId;
    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "\\d{6}", message = "OTP must contain exactly 6 digits")
    private String otp;

    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
}
