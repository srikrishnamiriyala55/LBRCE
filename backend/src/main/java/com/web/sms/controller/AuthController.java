package com.web.sms.controller;

import com.web.sms.dto.request.LoginRequest;
import com.web.sms.dto.request.StudentRegistrationRequest;
import com.web.sms.dto.response.LoginResponse;
import com.web.sms.dto.response.StudentProfileResponse;
import com.web.sms.service.AuthService;
import com.web.sms.service.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            @Valid @RequestBody StudentRegistrationRequest request) {
        return ResponseEntity.status(201).body(authService.registerStudent(request));
    }
}
