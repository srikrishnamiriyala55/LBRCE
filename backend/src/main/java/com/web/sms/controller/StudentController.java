package com.web.sms.controller;

import com.web.sms.dto.request.BusApplicationRequest;
import com.web.sms.dto.request.PaymentRequest;
import com.web.sms.dto.request.TransferRequestDto;
import com.web.sms.dto.response.*;
import com.web.sms.security.UserPrincipal;
import com.web.sms.service.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import com.web.sms.repository.StudentRepository;
import com.web.sms.repository.TransportAllocationRepository;
import com.web.sms.enums.EntityStatus;
import com.web.sms.exception.ResourceNotFoundException;

import java.util.List;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final StudentService studentService;
    private final BusService busService;
    private final ApplicationService applicationService;
    private final FeeService feeService;
    private final PassService passService;
    private final TransferService transferService;
    private final NotificationService notificationService;
    private final StudentRepository studentRepository;
    private final TransportAllocationRepository allocationRepository;

    public StudentController(StudentService studentService, BusService busService,
                             ApplicationService applicationService, FeeService feeService,
                             PassService passService, TransferService transferService,
                             NotificationService notificationService, StudentRepository studentRepository, TransportAllocationRepository allocationRepository) {
        this.studentService = studentService;
        this.busService = busService;
        this.applicationService = applicationService;
        this.feeService = feeService;
        this.passService = passService;
        this.transferService = transferService;
        this.notificationService = notificationService;
        this.studentRepository = studentRepository;
        this.allocationRepository = allocationRepository;
    }

    private UserPrincipal getCurrentUser() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private Pageable checked(Pageable pageable) {
        if (pageable.getPageNumber() < 0 || pageable.getPageSize() < 1 || pageable.getPageSize() > 50) {
            throw new com.web.sms.exception.BadRequestException("Page size must be between 1 and 50");
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
    }

    @GetMapping("/dashboard")
    public DashboardResponse getDashboard() {
        return studentService.getDashboard(getCurrentUser().getId());
    }

    @GetMapping("/profile")
    public StudentProfileResponse getProfile() {
        return studentService.getProfile(getCurrentUser().getId());
    }

    @GetMapping("/profile/photo") public ResponseEntity<byte[]> profilePhoto() {
        var student = studentRepository.findById(getCurrentUser().getId()).orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        if (student.getPhotoData() == null || student.getPhotoData().length == 0) throw new ResourceNotFoundException("Profile photo not found");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(student.getPhotoContentType())).body(student.getPhotoData());
    }

    @GetMapping("/incharge/photo") public ResponseEntity<byte[]> inchargePhoto() {
        var allocation = allocationRepository.findByStudentId(getCurrentUser().getId()).stream().filter(a -> a.getStatus() == EntityStatus.ACTIVE).findFirst().orElseThrow(() -> new ResourceNotFoundException("Assigned bus not found"));
        var incharge = allocation.getBus().getIncharge();
        if (incharge == null || incharge.getPhotoData() == null || incharge.getPhotoData().length == 0) throw new ResourceNotFoundException("In-Charge photo not found");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(incharge.getPhotoContentType())).body(incharge.getPhotoData());
    }

    @GetMapping("/buses")
    public List<BusResponse> getActiveBuses() {
        return busService.getAllActiveBuses();
    }

    @GetMapping("/buses/{busId}/boarding-points")
    public List<BoardingPointResponse> getBoardingPoints(@PathVariable Long busId) {
        return busService.getBoardingPointsByBusId(busId);
    }

    @PostMapping("/applications")
    public ApplicationResponse submitApplication(
            @Valid @RequestBody BusApplicationRequest req) {
        return applicationService.submitApplication(getCurrentUser().getId(), req);
    }

    @GetMapping("/applications")
    public Page<ApplicationResponse> getApplications(Pageable pageable) {
        return applicationService.getStudentApplications(getCurrentUser().getId(), checked(pageable));
    }

    @GetMapping("/fees")
    public List<FeeResponse> getFees() {
        return feeService.getStudentFees(getCurrentUser().getId());
    }

    @PostMapping("/payments")
    public PaymentResponse initiatePayment(@Valid @RequestBody PaymentRequest req) {
        return feeService.processPayment(getCurrentUser().getId(), req);
    }

    @GetMapping("/pass")
    public PassResponse getPass() {
        return passService.getStudentPass(getCurrentUser().getId());
    }

    @GetMapping("/pass/photo")
    public ResponseEntity<byte[]> getPassPhoto() {
        return passService.studentPhotoResponse(getCurrentUser().getId());
    }

    @GetMapping("/pass/download")
    public ResponseEntity<byte[]> downloadPass() {
        PassResponse pass = passService.getStudentPass(getCurrentUser().getId());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=bus-pass-" + pass.getRollNumber() + ".pdf")
                .body(passService.generatePdf(getCurrentUser().getId()));
    }

    @PostMapping("/transfers")
    public TransferResponse submitTransfer(@Valid @RequestBody TransferRequestDto req) {
        return transferService.submitTransfer(getCurrentUser().getId(), req);
    }

    @GetMapping("/transfers")
    public List<TransferResponse> getTransfers() {
        return transferService.getStudentTransfers(getCurrentUser().getId());
    }

    @GetMapping("/transfers/{id}")
    public TransferResponse getTransfer(@PathVariable Long id) {
        return transferService.getStudentTransfer(getCurrentUser().getId(), id);
    }

    @DeleteMapping("/transfers/{id}")
    public void cancelTransfer(@PathVariable Long id) {
        transferService.cancel(getCurrentUser().getId(), id);
    }

    @GetMapping("/notifications")
    public Page<NotificationResponse> getNotifications(Pageable pageable) {
        return notificationService.getNotifications(getCurrentUser().getUsername(), checked(pageable));
    }

    @PutMapping("/notifications/{id}/read")
    public void markNotificationRead(@PathVariable Long id) {
        notificationService.markAsRead(id, getCurrentUser().getUsername());
    }
}
