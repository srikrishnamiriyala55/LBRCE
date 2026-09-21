package com.web.sms.dto.response;

public class OtpChallengeResponse {
    private String challengeId;
    private String maskedEmail;
    private int expiresInSeconds;

    public OtpChallengeResponse(String challengeId, String maskedEmail, int expiresInSeconds) {
        this.challengeId = challengeId;
        this.maskedEmail = maskedEmail;
        this.expiresInSeconds = expiresInSeconds;
    }
    public String getChallengeId() { return challengeId; }
    public String getMaskedEmail() { return maskedEmail; }
    public int getExpiresInSeconds() { return expiresInSeconds; }
}
