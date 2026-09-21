package com.web.sms.dto.response;

public class LoginInitiationResponse {
    private boolean otpRequired;
    private LoginResponse login;
    private OtpChallengeResponse challenge;

    public static LoginInitiationResponse completed(LoginResponse login) {
        LoginInitiationResponse response = new LoginInitiationResponse();
        response.login = login;
        return response;
    }
    public static LoginInitiationResponse otpRequired(OtpChallengeResponse challenge) {
        LoginInitiationResponse response = new LoginInitiationResponse();
        response.otpRequired = true;
        response.challenge = challenge;
        return response;
    }
    public boolean isOtpRequired() { return otpRequired; }
    public LoginResponse getLogin() { return login; }
    public OtpChallengeResponse getChallenge() { return challenge; }
}
