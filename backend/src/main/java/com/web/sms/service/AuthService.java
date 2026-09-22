package com.web.sms.service;

import com.web.sms.dto.request.LoginRequest;
import com.web.sms.dto.request.StudentRegistrationRequest;
import com.web.sms.dto.response.*;
import com.web.sms.entity.*;
import com.web.sms.exception.BadRequestException;
import com.web.sms.repository.*;
import com.web.sms.security.JwtTokenProvider;
import com.web.sms.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final StudentRepository studentRepository;
    private final InchargeRepository inchargeRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider,
                       StudentRepository studentRepository,
                       InchargeRepository inchargeRepository, AdminRepository adminRepository,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.studentRepository = studentRepository;
        this.inchargeRepository = inchargeRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getRollNumber(), request.getPassword()));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return buildLogin(principal);
    }

    @Transactional
    public StudentProfileResponse registerStudent(StudentRegistrationRequest request) {
        String studentId = request.getRollNumber().trim().toUpperCase();
        String email = request.getEmail().trim().toLowerCase();
        validateNewStudent(studentId, email);

        Student student = new Student();
        student.setRollNumber(studentId);
        student.setName(request.getName().trim());
        student.setEmail(email);
        student.setPhoneNumber(request.getPhoneNumber().trim());
        student.setBranch(request.getBranch().trim().toUpperCase());
        student.setYear(request.getYear());
        student.setSemester(request.getSemester());
        student.setPassword(passwordEncoder.encode(request.getPassword()));
        student.setStatus("ACTIVE");
        Student saved = studentRepository.save(student);
        return StudentProfileResponse.fromStudent(saved);
    }

    private void validateNewStudent(String studentId, String email) {
        if (studentRepository.existsByRollNumber(studentId)) {
            throw new BadRequestException("An account already exists for student ID " + studentId);
        }
        if (studentRepository.existsByEmail(email)) {
            throw new BadRequestException("An account already exists for this email address");
        }
    }

    private LoginResponse buildLogin(UserPrincipal principal) {
        String email = switch (principal.getRole()) {
            case STUDENT -> studentRepository.findById(principal.getId()).map(Student::getEmail).orElse(null);
            case INCHARGE -> inchargeRepository.findById(principal.getId()).map(Incharge::getEmail).orElse(null);
            case ADMIN -> adminRepository.findById(principal.getId()).map(Admin::getEmail).orElse(null);
        };
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(principal.getId(), principal.getLoginId(),
                principal.getName(), principal.getRole(), email);
        return new LoginResponse(tokenProvider.generateToken(principal), userInfo);
    }
}
