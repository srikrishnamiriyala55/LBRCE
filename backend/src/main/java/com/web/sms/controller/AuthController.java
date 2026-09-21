package com.web.sms.controller;

import com.web.sms.dto.request.LoginRequest;
import com.web.sms.dto.request.StudentRegistrationRequest;
import com.web.sms.dto.request.OtpVerificationRequest;
import com.web.sms.dto.response.LoginInitiationResponse;
import com.web.sms.dto.response.OtpChallengeResponse;
import com.web.sms.dto.response.LoginResponse;
import com.web.sms.dto.response.StudentProfileResponse;
import com.web.sms.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginInitiationResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/login/verify-otp")
    public ResponseEntity<LoginResponse> verifyLoginOtp(@Valid @RequestBody OtpVerificationRequest request) {
        return ResponseEntity.ok(authService.verifyLoginOtp(request));
    }

    @PostMapping("/register/student")
    public ResponseEntity<OtpChallengeResponse> registerStudent(
            @Valid @RequestBody StudentRegistrationRequest request) {
        return ResponseEntity.ok(authService.requestStudentRegistration(request));
    }

    @PostMapping("/register/student/verify-otp")
    public ResponseEntity<StudentProfileResponse> verifyStudentOtp(
            @Valid @RequestBody OtpVerificationRequest request) {
        return ResponseEntity.status(201).body(authService.verifyStudentRegistration(request));
    }
}
