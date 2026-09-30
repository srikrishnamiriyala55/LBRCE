package com.web.sms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class LoginOtpVerificationRequest {
    @NotBlank private String challengeToken;
    @NotBlank @Pattern(regexp="^[0-9]{6}$", message="OTP must contain 6 digits") private String otp;
    public String getChallengeToken() { return challengeToken; }
    public void setChallengeToken(String challengeToken) { this.challengeToken = challengeToken; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
}
