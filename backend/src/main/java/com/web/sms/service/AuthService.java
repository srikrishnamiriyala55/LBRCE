package com.web.sms.service;

import com.web.sms.dto.request.LoginRequest;
import com.web.sms.dto.request.StudentRegistrationRequest;
import com.web.sms.dto.response.LoginResponse;
import com.web.sms.dto.response.StudentProfileResponse;
import com.web.sms.entity.Student;
import com.web.sms.exception.BadRequestException;
import com.web.sms.repository.StudentRepository;
import com.web.sms.security.JwtTokenProvider;
import com.web.sms.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider,
                       StudentRepository studentRepository, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getRollNumber(),
                        request.getPassword()
                )
        );

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String token = tokenProvider.generateToken(userPrincipal);
        
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                userPrincipal.getId(),
                userPrincipal.getLoginId(),
                userPrincipal.getName(),
                userPrincipal.getRole(),
                null // email omitted for brevity
        );

        return new LoginResponse(token, userInfo);
    }

    @Transactional
    public StudentProfileResponse registerStudent(StudentRegistrationRequest request) {
        String studentId = request.getRollNumber().trim().toUpperCase();
        String email = request.getEmail().trim().toLowerCase();

        if (studentRepository.existsByRollNumber(studentId)) {
            throw new BadRequestException("An account already exists for student ID " + studentId);
        }
        if (studentRepository.existsByEmail(email)) {
            throw new BadRequestException("An account already exists for this email address");
        }

        Student student = new Student();
        student.setRollNumber(studentId);
        student.setName(request.getName().trim());
        student.setEmail(email);
        student.setPhoneNumber(request.getPhoneNumber().trim());
        student.setBranch(request.getBranch().trim().toUpperCase());
        student.setYear(request.getYear());
        student.setSemester(request.getSemester());
        student.setPassword(passwordEncoder.encode(request.getPassword()));
        student.setStatus("PENDING");
        return StudentProfileResponse.fromStudent(studentRepository.save(student));
    }
}
