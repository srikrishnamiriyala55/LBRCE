package com.web.sms.service;

import com.web.sms.dto.request.LoginRequest;
import com.web.sms.dto.request.StudentRegistrationRequest;
import com.web.sms.dto.response.*;
import com.web.sms.entity.*;
import com.web.sms.exception.BadRequestException;
import com.web.sms.repository.*;
import com.web.sms.security.JwtTokenProvider;
import com.web.sms.security.UserPrincipal;
import com.web.sms.enums.Role;
import io.jsonwebtoken.Claims;
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
    private final OtpService otpService;

    public AuthService(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider,
                       StudentRepository studentRepository,
                       InchargeRepository inchargeRepository, AdminRepository adminRepository,
                       PasswordEncoder passwordEncoder, OtpService otpService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.studentRepository = studentRepository;
        this.inchargeRepository = inchargeRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getRollNumber(), request.getPassword()));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        if (principal.getRole() == Role.STUDENT) return buildLogin(principal);
        String email = getEmail(principal);
        if (email == null || email.isBlank()) throw new BadRequestException("This account has no email address. Contact the transport administrator.");
        otpService.send(email);
        LoginResponse response = new LoginResponse(null, buildUserInfo(principal, email));
        response.setOtpRequired(true);
        response.setOtpChallengeToken(tokenProvider.generateVerificationToken(principal.getUsername(), "LOGIN_OTP",
                java.util.Map.of("email", email, "role", principal.getRole().name(), "id", principal.getId()), 5 * 60 * 1000));
        return response;
    }

    public LoginResponse verifyLoginOtp(String challengeToken, String otp) {
        Claims claims;
        try { claims = tokenProvider.getVerifiedClaims(challengeToken, "LOGIN_OTP"); }
        catch (Exception e) { throw new BadRequestException("Your login verification expired. Please sign in again."); }
        String email = claims.get("email", String.class); otpService.verify(email, otp);
        Role role = Role.valueOf(claims.get("role", String.class));
        Long id = claims.get("id", Long.class);
        String loginId = claims.getSubject();
        UserPrincipal principal = new UserPrincipal(id, loginId, "", role,
                switch (role) { case ADMIN -> adminRepository.findById(id).map(Admin::getName).orElse(""); case INCHARGE -> inchargeRepository.findById(id).map(Incharge::getName).orElse(""); case STUDENT -> studentRepository.findById(id).map(Student::getName).orElse(""); }, "ACTIVE");
        return buildLogin(principal);
    }

    public void resendLoginOtp(String challengeToken) {
        try { otpService.send(tokenProvider.getVerifiedClaims(challengeToken, "LOGIN_OTP").get("email", String.class)); }
        catch (BadRequestException e) { throw e; }
        catch (Exception e) { throw new BadRequestException("Your login verification expired. Please sign in again."); }
    }

    public void sendRegistrationOtp(String email) { otpService.send(email.trim().toLowerCase()); }
    public String verifyRegistrationOtp(String email, String otp) {
        String normalized = email.trim().toLowerCase(); otpService.verify(normalized, otp);
        return tokenProvider.generateVerificationToken(normalized, "REGISTRATION_OTP", java.util.Map.of("email", normalized), 5 * 60 * 1000);
    }

    @Transactional
    public StudentProfileResponse registerStudent(StudentRegistrationRequest request, org.springframework.web.multipart.MultipartFile photo) {
        String studentId = request.getRollNumber().trim().toUpperCase();
        String email = request.getEmail().trim().toLowerCase();
        try { if (!email.equals(tokenProvider.getVerifiedClaims(request.getEmailVerificationToken(), "REGISTRATION_OTP").get("email", String.class))) throw new BadRequestException("Verify the email address used for registration"); }
        catch (BadRequestException e) { throw e; } catch (Exception e) { throw new BadRequestException("Email verification expired. Request a new OTP."); }
        validateNewStudent(studentId, email);
        validateAcademicProgress(request.getBranch(), request.getYear(), request.getSemester());

        Student student = new Student();
        student.setRollNumber(studentId);
        student.setName(request.getName().trim());
        student.setEmail(email);
        student.setPhoneNumber(request.getPhoneNumber().trim());
        student.setDob(request.getDob());
        student.setGender(request.getGender());
        student.setAddress(request.getAddress().trim());
        student.setBranch(request.getBranch().trim().toUpperCase());
        student.setYear(request.getYear());
        student.setSemester(request.getSemester());
        student.setBloodGroup(request.getBloodGroup());
        student.setParentName(request.getParentName().trim());
        student.setParentPhoneNumber(request.getParentPhoneNumber().trim());
        student.setPassword(passwordEncoder.encode(request.getPassword()));
        storePhoto(student, photo);
        student.setStatus("ACTIVE");
        Student saved = studentRepository.save(student);
        return StudentProfileResponse.fromStudent(saved);
    }

    private void storePhoto(Student student, org.springframework.web.multipart.MultipartFile photo) {
        if (photo == null || photo.isEmpty() || photo.getSize() > 2L * 1024 * 1024) throw new BadRequestException("A JPEG or PNG student photo up to 2 MB is required");
        try { byte[] b = photo.getBytes(); boolean jpeg=b.length>=3&&(b[0]&255)==255&&(b[1]&255)==216&&(b[2]&255)==255; boolean png=b.length>=8&&(b[0]&255)==137&&b[1]==80&&b[2]==78&&b[3]==71; if(!jpeg&&!png)throw new BadRequestException("Student photo must be a valid JPEG or PNG image"); student.setPhotoData(b);student.setPhotoContentType(jpeg?"image/jpeg":"image/png"); } catch (java.io.IOException e) { throw new BadRequestException("Unable to read the student photo"); }
    }

    public boolean studentRollNumberExists(String rollNumber) {
        if (rollNumber == null) return false;
        String normalized = rollNumber.trim().toUpperCase();
        if (normalized.isEmpty() || normalized.length() > 30) return false;
        return studentRepository.existsByRollNumber(normalized);
    }

    private void validateNewStudent(String studentId, String email) {
        if (studentRepository.existsByRollNumber(studentId)) {
            throw new BadRequestException("An account already exists for student ID " + studentId);
        }
        if (studentRepository.existsByEmail(email)) {
            throw new BadRequestException("An account already exists for this email address");
        }
    }

    private void validateAcademicProgress(String department, Integer year, Integer semester) {
        if (year == null || semester == null || (semester + 1) / 2 != year) {
            throw new BadRequestException("Semester must correspond to the selected study year");
        }
        if ("MBA".equalsIgnoreCase(department) && year > 2) {
            throw new BadRequestException("MBA is a two-year programme; select year 1 or 2");
        }
    }

    private LoginResponse buildLogin(UserPrincipal principal) {
        String email = getEmail(principal);
        return new LoginResponse(tokenProvider.generateToken(principal), buildUserInfo(principal, email));
    }
    private String getEmail(UserPrincipal principal) { return switch (principal.getRole()) {
            case STUDENT -> studentRepository.findById(principal.getId()).map(Student::getEmail).orElse(null);
            case INCHARGE -> inchargeRepository.findById(principal.getId()).map(Incharge::getEmail).orElse(null);
            case ADMIN -> adminRepository.findById(principal.getId()).map(Admin::getEmail).orElse(null);
        }; }
    private LoginResponse.UserInfo buildUserInfo(UserPrincipal principal, String email) { return new LoginResponse.UserInfo(principal.getId(), principal.getLoginId(), principal.getName(), principal.getRole(), email); }
}
