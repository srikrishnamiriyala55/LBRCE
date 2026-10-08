package com.web.sms.service;

import com.web.sms.dto.response.DashboardResponse;
import com.web.sms.dto.response.StudentProfileResponse;
import com.web.sms.entity.AcademicYear;
import com.web.sms.entity.BusPass;
import com.web.sms.entity.Fee;
import com.web.sms.entity.Student;
import com.web.sms.entity.TransportAllocation;
import com.web.sms.entity.StudentAcademicDetails;
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
import com.web.sms.repository.StudentAcademicDetailsRepository;
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
    private final StudentAcademicDetailsRepository academicDetailsRepository;

    public StudentServiceImpl(StudentRepository studentRepository,
                              TransportAllocationRepository transportAllocationRepository,
                              FeeRepository feeRepository,
                              BusPassRepository busPassRepository,
                              AcademicYearRepository academicYearRepository,
                              BusApplicationRepository applicationRepository,
                              TransferRequestRepository transferRepository, StudentAcademicDetailsRepository academicDetailsRepository) {
        this.studentRepository = studentRepository;
        this.transportAllocationRepository = transportAllocationRepository;
        this.feeRepository = feeRepository;
        this.busPassRepository = busPassRepository;
        this.academicYearRepository = academicYearRepository;
        this.applicationRepository = applicationRepository;
        this.transferRepository = transferRepository;
        this.academicDetailsRepository = academicDetailsRepository;
    }

    @Override
    public StudentProfileResponse getProfile(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));
        Optional<AcademicYear> activeYear = academicYearRepository.findByActiveTrue();
        Optional<StudentAcademicDetails> details = activeYear.flatMap(y -> academicDetailsRepository.findByStudentIdAndAcademicYearId(studentId, y.getId()));
        return StudentProfileResponse.fromStudent(student, details.orElse(null));
    }

    @Override
    public DashboardResponse getDashboard(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        DashboardResponse response = new DashboardResponse();
        response.put("studentName", student.getName());
        response.put("rollNumber", student.getRollNumber());
        response.put("status", student.getStatus());

        Optional<AcademicYear> activeYear = academicYearRepository.findByActiveTrue();
        if (activeYear.isPresent()) {
            Long yearId = activeYear.get().getId();
            response.put("academicYear", activeYear.get().getYearName());

            // Whether the student has been registered for the active academic year by admin
            boolean academicRegistered = academicDetailsRepository.findByStudentIdAndAcademicYearId(studentId, yearId).isPresent();
            response.put("activeAcademicYearRegistered", academicRegistered);

            academicDetailsRepository.findByStudentIdAndAcademicYearId(studentId, yearId).ifPresentOrElse(details -> {
                response.put("branch", details.getBranch());
                response.put("year", details.getYear());
                response.put("semester", details.getSemester());
            }, () -> response.put("academicProfileMissing", true));

            Optional<TransportAllocation> allocation = transportAllocationRepository
                    .findByStudentIdAndAcademicYearIdAndStatus(studentId, yearId, EntityStatus.ACTIVE);
            boolean hasActiveApplication = !applicationRepository.findByStudentIdAndAcademicYearIdAndStatusIn(
                    studentId, yearId, List.of(ApplicationStatus.PENDING, ApplicationStatus.UNDER_REVIEW, ApplicationStatus.APPROVED)).isEmpty();
            boolean hasActiveTransfer = !transferRepository.findByStudentIdAndAcademicYearIdAndStatusIn(
                    studentId, yearId, List.of(TransferStatus.TRANSFER_REQUESTED, TransferStatus.OLD_INCHARGE_APPROVED,
                            TransferStatus.NEW_INCHARGE_APPROVED)).isEmpty();

            // Detect renewal candidates: had allocation in any previous year but not the current year
            List<TransportAllocation> allPastAllocations = transportAllocationRepository.findByStudentId(studentId)
                    .stream().filter(a -> !a.getAcademicYear().getId().equals(yearId)).toList();
            boolean previousYearAllocated = !allPastAllocations.isEmpty();
            response.put("previousYearAllocated", previousYearAllocated);

            boolean needsRenewal = previousYearAllocated && allocation.isEmpty() && !hasActiveApplication;
            response.put("needsRenewal", needsRenewal);

            if (allocation.isPresent()) {
                TransportAllocation alloc = allocation.get();
                response.put("transportationStatus", "ALLOCATED");
                response.put("busNumber", alloc.getBus().getBusNumber());
                response.put("startingPoint", alloc.getBus().getStartingPoint());
                response.put("endingPoint", alloc.getBus().getEndingPoint());
                if (alloc.getBus().getIncharge() != null) {
                    var incharge = alloc.getBus().getIncharge();
                    response.put("inchargeName", incharge.getName());
                    response.put("inchargeDesignation", incharge.getDesignation());
                    response.put("inchargeBranch", incharge.getDepartment());
                    response.put("inchargePhoneNumber", incharge.getPhoneNumber());
                    response.put("inchargePhotoAvailable", incharge.getPhotoData() != null && incharge.getPhotoData().length > 0);
                }
                if (alloc.getBoardingPoint() != null) {
                    response.put("boardingPoint", alloc.getBoardingPoint().getStationName());
                }
            } else {
                response.put("transportationStatus", "NOT_ALLOCATED");
                // Surface the most recent past allocation for the renewal banner reference
                if (previousYearAllocated) {
                    allPastAllocations.stream()
                            .max(java.util.Comparator.comparing(a -> a.getAcademicYear().getId()))
                            .ifPresent(prev -> {
                                response.put("previousBusNumber", prev.getBus().getBusNumber());
                                response.put("previousBoardingPoint",
                                        prev.getBoardingPoint() != null ? prev.getBoardingPoint().getStationName() : null);
                                response.put("previousAcademicYear", prev.getAcademicYear().getYearName());
                            });
                }
            }
            long usedTransfers = transferRepository.countByStudentIdAndAcademicYearIdAndStatusNot(studentId, yearId, TransferStatus.CANCELLED);
            boolean transferLimitReached = usedTransfers >= 2;
            response.put("usedTransfersCount", usedTransfers);
            response.put("transferLimitReached", transferLimitReached);
            response.put("canApply", allocation.isEmpty() && !hasActiveApplication);
            response.put("canTransfer", allocation.isPresent() && !hasActiveTransfer && !transferLimitReached);
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
            response.put("needsRenewal", false);
            response.put("previousYearAllocated", false);
            response.put("activeAcademicYearRegistered", false);
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
