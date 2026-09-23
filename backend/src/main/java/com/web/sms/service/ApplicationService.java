package com.web.sms.service;

import com.web.sms.dto.request.BusApplicationRequest;
import com.web.sms.dto.request.UpdateApplicationRequest;
import com.web.sms.dto.response.ApplicationResponse;
import com.web.sms.entity.*;
import com.web.sms.enums.ApplicationStatus;
import com.web.sms.enums.EntityStatus;
import com.web.sms.exception.BadRequestException;
import com.web.sms.exception.ResourceNotFoundException;
import com.web.sms.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApplicationService {

    private final BusApplicationRepository appRepo;
    private final StudentRepository studentRepo;
    private final BusRepository busRepo;
    private final BoardingPointsRepository boardingPointRepo;
    private final AcademicYearRepository academicYearRepo;
    private final TransportAllocationRepository allocationRepo;
    private final FeeRepository feeRepo;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public ApplicationService(BusApplicationRepository appRepo,
                              StudentRepository studentRepo,
                              BusRepository busRepo,
                              BoardingPointsRepository boardingPointRepo,
                              AcademicYearRepository academicYearRepo,
                              TransportAllocationRepository allocationRepo,
                              FeeRepository feeRepo,
                              NotificationService notificationService, AuditService auditService) {
        this.appRepo = appRepo;
        this.studentRepo = studentRepo;
        this.busRepo = busRepo;
        this.boardingPointRepo = boardingPointRepo;
        this.academicYearRepo = academicYearRepo;
        this.allocationRepo = allocationRepo;
        this.feeRepo = feeRepo;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional
    public ApplicationResponse submitApplication(Long studentId, BusApplicationRequest req, MultipartFile photo) {
        Student student = studentRepo.findByIdForUpdate(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (!"ACTIVE".equalsIgnoreCase(student.getStatus())) {
            throw new BadRequestException("Only active students can apply for transportation");
        }

        updateStudentDetails(student, req);
        storeStudentPhoto(student, photo);

        AcademicYear year = academicYearRepo.findByActiveTrue()
                .orElseThrow(() -> new BadRequestException("No active academic year found in the system"));

        if (allocationRepo.findByStudentIdAndAcademicYearIdAndStatus(studentId, year.getId(), EntityStatus.ACTIVE).isPresent()) {
            throw new BadRequestException("A bus is already allocated to you. Use the transfer workflow to request a different bus");
        }

        Bus bus = busRepo.findById(req.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Selected bus not found"));

        if (bus.getStatus() != EntityStatus.ACTIVE || bus.getIncharge() == null ||
                !"ACTIVE".equalsIgnoreCase(bus.getIncharge().getStatus())) {
            throw new BadRequestException("The selected bus is not currently accepting applications");
        }

        BoardingPoints boardingPoint = boardingPointRepo.findById(req.getBoardingPointId())
                .orElseThrow(() -> new ResourceNotFoundException("Boarding point not found"));

        if (!boardingPoint.getBus().getId().equals(bus.getId())) {
            throw new BadRequestException("The selected boarding point does not belong to the selected bus");
        }
        if (boardingPoint.getStatus() != EntityStatus.ACTIVE) {
            throw new BadRequestException("The selected boarding point is inactive");
        }

        List<BusApplication> activeApps = appRepo.findByStudentIdAndAcademicYearIdAndStatusIn(
                studentId, year.getId(), List.of(ApplicationStatus.PENDING, ApplicationStatus.UNDER_REVIEW, ApplicationStatus.APPROVED)
        );
        if (!activeApps.isEmpty()) {
            throw new BadRequestException("You already have an active or approved application for this academic year");
        }

        BusApplication app = new BusApplication();
        app.setStudent(student);
        app.setBus(bus);
        app.setBoardingPoint(boardingPoint);
        app.setAcademicYear(year);
        app.setStatus(ApplicationStatus.PENDING);
        app.setAppliedAt(LocalDateTime.now());
        app.setCreatedAt(LocalDateTime.now());

        BusApplication saved = appRepo.save(app);

        notificationService.createNotification(
                student.getRollNumber(),
                "Application Submitted",
                "Your bus application for " + bus.getBusNumber() + " at " + boardingPoint.getStationName() + " has been submitted.",
                "APPLICATION"
        );
        if (bus.getIncharge() != null) notificationService.createNotification(bus.getIncharge().getTeacherId(), "New Bus Application", "New student application received for your bus.", "APPLICATION");

        return ApplicationResponse.fromApplication(saved);
    }

    private void storeStudentPhoto(Student student, MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new BadRequestException("A recent student photo is required");
        }
        if (photo.getSize() > 2L * 1024 * 1024) {
            throw new BadRequestException("Student photo must not exceed 2 MB");
        }
        byte[] bytes;
        try {
            bytes = photo.getBytes();
        } catch (IOException exception) {
            throw new BadRequestException("Unable to read the uploaded student photo");
        }
        boolean jpeg = bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
        boolean png = bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47;
        if (!jpeg && !png) {
            throw new BadRequestException("Student photo must be a valid JPEG or PNG image");
        }
        student.setPhotoData(bytes);
        student.setPhotoContentType(jpeg ? MediaType.IMAGE_JPEG_VALUE : MediaType.IMAGE_PNG_VALUE);
        studentRepo.save(student);
    }

    private void updateStudentDetails(Student student, BusApplicationRequest req) {
        if ((req.getSemester() + 1) / 2 != req.getYear()) {
            throw new BadRequestException("Semester must correspond to the selected study year");
        }
        String email = req.getEmail().trim().toLowerCase();
        if (studentRepo.existsByEmailAndIdNot(email, student.getId())) {
            throw new BadRequestException("Another student account already uses this email address");
        }
        student.setName(req.getName().trim());
        student.setEmail(email);
        student.setPhoneNumber(req.getPhoneNumber().trim());
        student.setDob(req.getDob());
        student.setGender(req.getGender());
        student.setAddress(req.getAddress().trim());
        student.setBranch(req.getBranch().trim().toUpperCase());
        student.setYear(req.getYear());
        student.setSemester(req.getSemester());
        student.setBloodGroup(req.getBloodGroup());
        student.setParentName(req.getParentName().trim());
        student.setParentPhoneNumber(req.getParentPhoneNumber().trim());
    }

    public Page<ApplicationResponse> getStudentApplications(Long studentId, Pageable pageable) {
        return appRepo.findByStudentId(studentId, pageable)
                .map(ApplicationResponse::fromApplication);
    }

    @Transactional
    public ApplicationResponse updateApplication(Long applicationId,UpdateApplicationRequest req,String actor){
        BusApplication app=appRepo.findByIdForUpdate(applicationId).orElseThrow(()->new ResourceNotFoundException("Application not found"));
        if(app.getStatus()!=ApplicationStatus.PENDING&&app.getStatus()!=ApplicationStatus.UNDER_REVIEW)throw new BadRequestException("Only pending applications can be edited");
        Bus bus=busRepo.findById(req.getBusId()).orElseThrow(()->new ResourceNotFoundException("Selected bus not found"));
        BoardingPoints point=boardingPointRepo.findById(req.getBoardingPointId()).orElseThrow(()->new ResourceNotFoundException("Boarding point not found"));
        if(bus.getStatus()!=EntityStatus.ACTIVE||bus.getIncharge()==null||!"ACTIVE".equalsIgnoreCase(bus.getIncharge().getStatus()))throw new BadRequestException("The selected bus is not accepting applications");
        if(!point.getBus().getId().equals(bus.getId())||point.getStatus()!=EntityStatus.ACTIVE)throw new BadRequestException("Select an active boarding point belonging to the selected bus");
        String old=app.getBus().getBusNumber()+" / "+app.getBoardingPoint().getStationName();
        app.setBus(bus);app.setBoardingPoint(point);app.setRemarks(req.getRemarks()==null?null:req.getRemarks().trim());
        BusApplication saved=appRepo.save(app);auditService.log(actor,"ADMIN","APPLICATION_UPDATED","BUS_APPLICATION",String.valueOf(applicationId),old,bus.getBusNumber()+" / "+point.getStationName(),null);return ApplicationResponse.fromApplication(saved);
    }

    @Transactional
    public void deleteApplication(Long applicationId,String actor){
        BusApplication app=appRepo.findByIdForUpdate(applicationId).orElseThrow(()->new ResourceNotFoundException("Application not found"));
        if(app.getStatus()==ApplicationStatus.APPROVED||allocationRepo.existsByApplicationId(applicationId))throw new BadRequestException("Approved or allocated applications cannot be deleted");
        appRepo.delete(app);auditService.log(actor,"ADMIN","APPLICATION_DELETED","BUS_APPLICATION",String.valueOf(applicationId),app.getStudent().getRollNumber()+" / "+app.getBus().getBusNumber(),null,null);
    }

    @Transactional
    public ApplicationResponse approveApplication(Long applicationId, String reviewerId) {
        return approveApplication(applicationId, reviewerId, null);
    }

    @Transactional
    public ApplicationResponse approveApplication(Long applicationId, String reviewerId, Long authorizedBusId) {
        BusApplication app = appRepo.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        if (authorizedBusId != null && !app.getBus().getId().equals(authorizedBusId)) {
            throw new com.web.sms.exception.ForbiddenException("This application does not belong to your assigned bus");
        }

        if (app.getStatus() == ApplicationStatus.APPROVED) {
            return ApplicationResponse.fromApplication(app);
        }
        if (app.getStatus() != ApplicationStatus.PENDING && app.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new BadRequestException("Only pending applications can be approved");
        }

        Bus bus = busRepo.findByIdForUpdate(app.getBus().getId()).orElseThrow(() -> new ResourceNotFoundException("Bus not found"));
        java.util.Optional<TransportAllocation> existingAllocation = allocationRepo.findActiveForUpdate(
                app.getStudent().getId(), app.getAcademicYear().getId());
        if (existingAllocation.isPresent()) {
            TransportAllocation existing = existingAllocation.get();
            if (!existing.getBus().getId().equals(bus.getId()) ||
                    !existing.getBoardingPoint().getId().equals(app.getBoardingPoint().getId())) {
                throw new BadRequestException("Student already has an active allocation for a different bus or boarding point; use the transfer workflow");
            }
            app.setStatus(ApplicationStatus.APPROVED);
            app.setReviewedAt(LocalDateTime.now());
            app.setReviewedBy(reviewerId);
            existing.setApplication(app);
            allocationRepo.save(existing);
            BusApplication reconciled = appRepo.save(app);
            notificationService.createNotification(app.getStudent().getRollNumber(), "Application Approved",
                    "Your existing " + bus.getBusNumber() + " transportation assignment has been verified and approved.", "APPLICATION");
            auditService.log(reviewerId,"INCHARGE","APPLICATION_APPROVED","BUS_APPLICATION",String.valueOf(app.getId()),"PENDING","APPROVED",null);
            return ApplicationResponse.fromApplication(reconciled);
        }

        long occupied = allocationRepo.countByBusIdAndStatus(bus.getId(), EntityStatus.ACTIVE);
        int capacity = bus.getTotalSeats() != null ? bus.getTotalSeats() : 0;
        if (bus.getStatus() != EntityStatus.ACTIVE || bus.getIncharge() == null ||
                !"ACTIVE".equalsIgnoreCase(bus.getIncharge().getStatus()) ||
                app.getBoardingPoint().getStatus() != EntityStatus.ACTIVE) {
            throw new BadRequestException("The selected bus or boarding point is no longer active");
        }
        if (occupied >= capacity || (bus.getAvailableSeats() != null && bus.getAvailableSeats() <= 0)) {
            throw new BadRequestException("Bus " + bus.getBusNumber() + " has no available seats");
        }
        app.setStatus(ApplicationStatus.APPROVED);
        app.setReviewedAt(LocalDateTime.now());
        app.setReviewedBy(reviewerId);
        BusApplication updatedApp = appRepo.save(app);

        TransportAllocation allocation = new TransportAllocation();
        allocation.setStudent(app.getStudent());
        allocation.setBus(app.getBus());
        allocation.setBoardingPoint(app.getBoardingPoint());
        allocation.setAcademicYear(app.getAcademicYear());
        allocation.setApplication(updatedApp);
        allocation.setStatus(EntityStatus.ACTIVE);
        allocation.setAllocatedAt(LocalDateTime.now());
        allocation.setCreatedAt(LocalDateTime.now());
        TransportAllocation savedAllocation = allocationRepo.save(allocation);

        if (bus.getAvailableSeats() != null && bus.getAvailableSeats() > 0) {
            bus.setAvailableSeats(bus.getAvailableSeats() - 1);
            busRepo.save(bus);
        }

        Fee fee = new Fee();
        fee.setStudent(app.getStudent());
        fee.setAcademicYear(app.getAcademicYear());
        fee.setAllocation(savedAllocation);
        fee.setTotalAmount(app.getBoardingPoint().getFeeAmount() != null ? app.getBoardingPoint().getFeeAmount() : 5000L);
        fee.setPaidAmount(0L);
        fee.setStatus("PENDING");
        fee.setPassEligible(false);
        fee.setCreatedAt(LocalDateTime.now());
        feeRepo.save(fee);

        notificationService.createNotification(
                app.getStudent().getRollNumber(),
                "Application Approved",
                "Your bus transportation application for " + bus.getBusNumber() + " has been APPROVED. Transportation fee has been generated.",
                "APPLICATION"
        );
        auditService.log(reviewerId,"INCHARGE","APPLICATION_APPROVED","BUS_APPLICATION",String.valueOf(app.getId()),"PENDING","APPROVED",null);

        return ApplicationResponse.fromApplication(updatedApp);
    }

    @Transactional
    public ApplicationResponse rejectApplication(Long applicationId, String remarks, String reviewerId) {
        return rejectApplication(applicationId, remarks, reviewerId, null);
    }

    @Transactional
    public ApplicationResponse rejectApplication(Long applicationId, String remarks, String reviewerId, Long authorizedBusId) {
        BusApplication app = appRepo.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        if (authorizedBusId != null && !app.getBus().getId().equals(authorizedBusId)) {
            throw new com.web.sms.exception.ForbiddenException("This application does not belong to your assigned bus");
        }

        if (app.getStatus() != ApplicationStatus.PENDING && app.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new BadRequestException("Only pending applications can be rejected");
        }

        app.setStatus(ApplicationStatus.REJECTED);
        app.setRemarks(remarks);
        app.setReviewedAt(LocalDateTime.now());
        app.setReviewedBy(reviewerId);
        BusApplication updatedApp = appRepo.save(app);

        notificationService.createNotification(
                app.getStudent().getRollNumber(),
                "Application Rejected",
                "Your bus application for " + app.getBus().getBusNumber() + " was rejected. Reason: " + remarks,
                "APPLICATION"
        );
        auditService.log(reviewerId,"INCHARGE","APPLICATION_REJECTED","BUS_APPLICATION",String.valueOf(app.getId()),"PENDING","REJECTED",null);

        return ApplicationResponse.fromApplication(updatedApp);
    }

    public Page<ApplicationResponse> getApplicationsByBus(Long busId, ApplicationStatus status, Pageable pageable) {
        if (status != null) {
            return appRepo.findByBusIdAndStatus(busId, status, pageable)
                    .map(ApplicationResponse::fromApplication);
        }
        return appRepo.findByBusId(busId, pageable)
                .map(ApplicationResponse::fromApplication);
    }
}
