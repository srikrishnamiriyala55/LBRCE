package com.web.sms.service;

import com.web.sms.dto.response.DashboardResponse;
import com.web.sms.dto.response.StudentProfileResponse;
import com.web.sms.entity.AcademicYear;
import com.web.sms.entity.BusPass;
import com.web.sms.entity.Fee;
import com.web.sms.entity.Student;
import com.web.sms.entity.TransportAllocation;
import com.web.sms.enums.EntityStatus;
import com.web.sms.enums.PassStatus;
import com.web.sms.exception.ResourceNotFoundException;
import com.web.sms.repository.AcademicYearRepository;
import com.web.sms.repository.BusPassRepository;
import com.web.sms.repository.FeeRepository;
import com.web.sms.repository.StudentRepository;
import com.web.sms.repository.TransportAllocationRepository;
import com.web.sms.repository.BusApplicationRepository;
import com.web.sms.repository.TransferRequestRepository;
import com.web.sms.enums.ApplicationStatus;
import com.web.sms.enums.TransferStatus;
import java.util.List;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final TransportAllocationRepository transportAllocationRepository;
    private final FeeRepository feeRepository;
    private final BusPassRepository busPassRepository;
    private final AcademicYearRepository academicYearRepository;
    private final BusApplicationRepository applicationRepository;
    private final TransferRequestRepository transferRepository;

    public StudentServiceImpl(StudentRepository studentRepository,
                              TransportAllocationRepository transportAllocationRepository,
                              FeeRepository feeRepository,
                              BusPassRepository busPassRepository,
                              AcademicYearRepository academicYearRepository,
                              BusApplicationRepository applicationRepository,
                              TransferRequestRepository transferRepository) {
        this.studentRepository = studentRepository;
        this.transportAllocationRepository = transportAllocationRepository;
        this.feeRepository = feeRepository;
        this.busPassRepository = busPassRepository;
        this.academicYearRepository = academicYearRepository;
        this.applicationRepository = applicationRepository;
        this.transferRepository = transferRepository;
    }

    @Override
    public StudentProfileResponse getProfile(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        return StudentProfileResponse.fromStudent(student);
    }

    @Override
    public DashboardResponse getDashboard(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        DashboardResponse response = new DashboardResponse();
        response.put("studentName", student.getName());
        response.put("rollNumber", student.getRollNumber());
        response.put("branch", student.getBranch());
        response.put("year", student.getYear());
        response.put("status", student.getStatus());

        Optional<AcademicYear> activeYear = academicYearRepository.findByActiveTrue();
        if (activeYear.isPresent()) {
            Long yearId = activeYear.get().getId();
            response.put("academicYear", activeYear.get().getYearName());

            Optional<TransportAllocation> allocation = transportAllocationRepository
                    .findByStudentIdAndAcademicYearIdAndStatus(studentId, yearId, EntityStatus.ACTIVE);
            boolean hasActiveApplication = !applicationRepository.findByStudentIdAndAcademicYearIdAndStatusIn(
                    studentId, yearId, List.of(ApplicationStatus.PENDING, ApplicationStatus.UNDER_REVIEW, ApplicationStatus.APPROVED)).isEmpty();
            boolean hasActiveTransfer = !transferRepository.findByStudentIdAndAcademicYearIdAndStatusIn(
                    studentId, yearId, List.of(TransferStatus.TRANSFER_REQUESTED, TransferStatus.OLD_INCHARGE_APPROVED,
                            TransferStatus.NEW_INCHARGE_APPROVED)).isEmpty();

            if (allocation.isPresent()) {
                TransportAllocation alloc = allocation.get();
                response.put("transportationStatus", "ALLOCATED");
                response.put("busNumber", alloc.getBus().getBusNumber());
                response.put("startingPoint", alloc.getBus().getStartingPoint());
                response.put("endingPoint", alloc.getBus().getEndingPoint());
                if (alloc.getBoardingPoint() != null) {
                    response.put("boardingPoint", alloc.getBoardingPoint().getStationName());
                }
            } else {
                response.put("transportationStatus", "NOT_ALLOCATED");
            }
            response.put("canApply", allocation.isEmpty() && !hasActiveApplication);
            response.put("canTransfer", allocation.isPresent() && !hasActiveTransfer);
            response.put("hasActiveApplication", hasActiveApplication);
            response.put("hasActiveTransfer", hasActiveTransfer);

            Optional<Fee> fee = feeRepository.findByStudentIdAndAcademicYearId(studentId, yearId);
            if (fee.isPresent()) {
                Fee f = fee.get();
                response.put("feeStatus", f.getStatus());
                response.put("totalFee", f.getTotalAmount());
                response.put("paidAmount", f.getPaidAmount());
                response.put("paidPercentage", f.getPaidPercentage());
                response.put("passEligible", f.isPassEligible());
            }

            Optional<BusPass> pass = busPassRepository
                    .findByStudentIdAndAcademicYearIdAndStatus(studentId, yearId, PassStatus.ACTIVE);
            if (pass.isPresent()) {
                response.put("passStatus", "ACTIVE");
                response.put("passNumber", pass.get().getPassNumber());
            } else {
                response.put("passStatus", "INACTIVE");
            }
        } else {
            response.put("transportationStatus", "NO_ACTIVE_ACADEMIC_YEAR");
            response.put("canApply", false);
            response.put("canTransfer", false);
        }

        return response;
    }

    @Override
    public StudentProfileResponse updateProfile(Long studentId, StudentProfileResponse dto) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        if (dto.getPhoneNumber() != null) student.setPhoneNumber(dto.getPhoneNumber());
        if (dto.getAddress() != null) student.setAddress(dto.getAddress());
        if (dto.getParentPhoneNumber() != null) student.setParentPhoneNumber(dto.getParentPhoneNumber());
        if (dto.getBloodGroup() != null) student.setBloodGroup(dto.getBloodGroup());

        Student updated = studentRepository.save(student);
        return StudentProfileResponse.fromStudent(updated);
    }
}
