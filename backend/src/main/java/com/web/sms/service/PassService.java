package com.web.sms.service;

import com.web.sms.dto.response.PassResponse;
import com.web.sms.entity.AcademicYear;
import com.web.sms.entity.BusPass;
import com.web.sms.enums.PassStatus;
import com.web.sms.exception.ResourceNotFoundException;
import com.web.sms.repository.AcademicYearRepository;
import com.web.sms.repository.BusPassRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PassService {

    private final BusPassRepository passRepo;
    private final AcademicYearRepository academicYearRepo;

    public PassService(BusPassRepository passRepo, AcademicYearRepository academicYearRepo) {
        this.passRepo = passRepo;
        this.academicYearRepo = academicYearRepo;
    }

    public PassResponse getStudentPass(Long studentId) {
        Optional<AcademicYear> activeYear = academicYearRepo.findByActiveTrue();
        if (activeYear.isEmpty()) {
            throw new ResourceNotFoundException("No active academic year configured");
        }

        BusPass pass = passRepo
                .findByStudentIdAndAcademicYearIdAndStatus(studentId, activeYear.get().getId(), PassStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("No active bus pass found for current academic year"));

        return PassResponse.fromBusPass(pass);
    }

    public PassResponse verifyPass(String token) {
        BusPass pass = passRepo.findByVerificationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or revoked bus pass verification token"));

        return PassResponse.fromBusPass(pass);
    }
}
