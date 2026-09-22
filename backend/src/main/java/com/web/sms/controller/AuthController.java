package com.web.sms.controller;

import com.web.sms.dto.request.LoginRequest;
import com.web.sms.dto.request.StudentRegistrationRequest;
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
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register/student")
    public ResponseEntity<StudentProfileResponse> registerStudent(
            @Valid @RequestBody StudentRegistrationRequest request) {
        return ResponseEntity.status(201).body(authService.registerStudent(request));
    }
}
