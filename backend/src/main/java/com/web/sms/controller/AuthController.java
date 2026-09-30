package com.web.sms.controller;

import com.web.sms.dto.request.LoginRequest;
import com.web.sms.dto.request.StudentRegistrationRequest;
import com.web.sms.dto.request.OtpRequest;
import com.web.sms.dto.request.LoginOtpVerificationRequest;
import com.web.sms.dto.response.LoginResponse;
import com.web.sms.dto.response.StudentProfileResponse;
import com.web.sms.service.AuthService;
import com.web.sms.service.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginAttemptService loginAttemptService;

    public AuthController(AuthService authService, LoginAttemptService loginAttemptService) {
        this.authService = authService;
        this.loginAttemptService = loginAttemptService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String remoteAddress = servletRequest.getRemoteAddr();
        loginAttemptService.verifyAllowed(request.getRollNumber(), remoteAddress);
        try {
            LoginResponse response = authService.login(request);
            loginAttemptService.recordSuccess(request.getRollNumber(), remoteAddress);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException ex) {
            loginAttemptService.recordFailure(request.getRollNumber(), remoteAddress);
            throw ex;
        }
    }

    @PostMapping("/register/student")
    public ResponseEntity<StudentProfileResponse> registerStudent(
            @Valid @RequestPart("student") StudentRegistrationRequest request, @RequestPart("photo") org.springframework.web.multipart.MultipartFile photo) {
        return ResponseEntity.status(201).body(authService.registerStudent(request, photo));
    }

    @PostMapping("/registration-otp/send") public Map<String, Boolean> sendRegistrationOtp(@Valid @RequestBody OtpRequest request) { authService.sendRegistrationOtp(request.getEmail()); return Map.of("success", true); }
    @PostMapping("/registration-otp/verify") public Map<String, String> verifyRegistrationOtp(@Valid @RequestBody OtpRequest request) { return Map.of("verificationToken", authService.verifyRegistrationOtp(request.getEmail(), request.getOtp())); }
    @PostMapping("/login/verify-otp") public ResponseEntity<LoginResponse> verifyLoginOtp(@Valid @RequestBody LoginOtpVerificationRequest request) { return ResponseEntity.ok(authService.verifyLoginOtp(request.getChallengeToken(), request.getOtp())); }

    @GetMapping("/register/student/roll-number-exists")
    public Map<String, Boolean> studentRollNumberExists(@RequestParam String rollNumber) {
        return Map.of("exists", authService.studentRollNumberExists(rollNumber));
    }
}
