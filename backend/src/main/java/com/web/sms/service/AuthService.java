package com.web.sms.service;

import com.web.sms.dto.request.LoginRequest;
import com.web.sms.dto.request.OtpVerificationRequest;
import com.web.sms.dto.request.StudentRegistrationRequest;
import com.web.sms.dto.response.*;
import com.web.sms.entity.*;
import com.web.sms.enums.Role;
import com.web.sms.exception.BadRequestException;
import com.web.sms.repository.*;
import com.web.sms.security.CustomUserDetailsService;
import com.web.sms.security.JwtTokenProvider;
import com.web.sms.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final StudentRepository studentRepository;
    private final InchargeRepository inchargeRepository;
    private final AdminRepository adminRepository;
    private final PendingStudentRegistrationRepository pendingRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    public AuthService(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider,
                       CustomUserDetailsService userDetailsService, StudentRepository studentRepository,
                       InchargeRepository inchargeRepository, AdminRepository adminRepository,
                       PendingStudentRegistrationRepository pendingRepository,
                       PasswordEncoder passwordEncoder, OtpService otpService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.studentRepository = studentRepository;
        this.inchargeRepository = inchargeRepository;
        this.adminRepository = adminRepository;
        this.pendingRepository = pendingRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
    }

    public LoginInitiationResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getRollNumber(), request.getPassword()));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        if (principal.getRole() == Role.STUDENT) {
            return LoginInitiationResponse.completed(buildLogin(principal));
        }
        String email = principal.getRole() == Role.ADMIN
                ? adminRepository.findById(principal.getId()).map(Admin::getEmail).orElse(null)
                : inchargeRepository.findById(principal.getId()).map(Incharge::getEmail).orElse(null);
        OtpChallengeResponse challenge = otpService.create("LOGIN_" + principal.getRole().name(),
                principal.getLoginId(), email);
        return LoginInitiationResponse.otpRequired(challenge);
    }

    public LoginResponse verifyLoginOtp(OtpVerificationRequest request) {
        OtpChallenge challenge = otpService.verify(request.getChallengeId(), request.getOtp(),
                Set.of("LOGIN_ADMIN", "LOGIN_INCHARGE"));
        UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername(challenge.getPrincipalId());
        if (!("LOGIN_" + principal.getRole().name()).equals(challenge.getPurpose())) {
            throw new BadRequestException("Verification request does not match this account");
        }
        return buildLogin(principal);
    }

    @Transactional
    public OtpChallengeResponse requestStudentRegistration(StudentRegistrationRequest request) {
        String studentId = request.getRollNumber().trim().toUpperCase();
        String email = request.getEmail().trim().toLowerCase();
        validateNewStudent(studentId, email);

        pendingRepository.findByRollNumberIgnoreCase(studentId).ifPresent(existing -> {
            pendingRepository.delete(existing);
        });
        pendingRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            pendingRepository.delete(existing);
        });
        pendingRepository.flush();

        OtpChallengeResponse response = otpService.create("STUDENT_REGISTRATION", studentId, email);
        PendingStudentRegistration pending = new PendingStudentRegistration();
        pending.setChallengeId(response.getChallengeId());
        pending.setRollNumber(studentId);
        pending.setName(request.getName().trim());
        pending.setEmail(email);
        pending.setPhoneNumber(request.getPhoneNumber().trim());
        pending.setBranch(request.getBranch().trim().toUpperCase());
        pending.setYear(request.getYear());
        pending.setSemester(request.getSemester());
        pending.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        pending.setCreatedAt(LocalDateTime.now());
        pendingRepository.save(pending);
        return response;
    }

    @Transactional
    public StudentProfileResponse verifyStudentRegistration(OtpVerificationRequest request) {
        OtpChallenge challenge = otpService.verify(request.getChallengeId(), request.getOtp(),
                Set.of("STUDENT_REGISTRATION"));
        PendingStudentRegistration pending = pendingRepository.findById(challenge.getId())
                .orElseThrow(() -> new BadRequestException("Pending student registration was not found"));
        validateNewStudent(pending.getRollNumber(), pending.getEmail());

        Student student = new Student();
        student.setRollNumber(pending.getRollNumber());
        student.setName(pending.getName());
        student.setEmail(pending.getEmail());
        student.setPhoneNumber(pending.getPhoneNumber());
        student.setBranch(pending.getBranch());
        student.setYear(pending.getYear());
        student.setSemester(pending.getSemester());
        student.setPassword(pending.getPasswordHash());
        student.setStatus("ACTIVE");
        Student saved = studentRepository.save(student);
        pendingRepository.delete(pending);
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
