package com.web.sms.controller;

import com.web.sms.dto.request.BusApplicationRequest;
import com.web.sms.dto.request.ComplaintRequest;
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
    private final ComplaintService complaintService;
    private final NotificationService notificationService;

    public StudentController(StudentService studentService, BusService busService,
                             ApplicationService applicationService, FeeService feeService,
                             PassService passService, TransferService transferService,
                             ComplaintService complaintService, NotificationService notificationService) {
        this.studentService = studentService;
        this.busService = busService;
        this.applicationService = applicationService;
        this.feeService = feeService;
        this.passService = passService;
        this.transferService = transferService;
        this.complaintService = complaintService;
        this.notificationService = notificationService;
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

    @PutMapping("/profile")
    public StudentProfileResponse updateProfile(@Valid @RequestBody StudentProfileResponse request) {
        return studentService.updateProfile(getCurrentUser().getId(), request);
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
    public ApplicationResponse submitApplication(@Valid @RequestBody BusApplicationRequest req) {
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

    @PostMapping("/complaints")
    public ComplaintResponse submitComplaint(@Valid @RequestBody ComplaintRequest req) {
        return complaintService.submitComplaint(getCurrentUser().getId(), req);
    }

    @GetMapping("/complaints")
    public Page<ComplaintResponse> getComplaints(Pageable pageable) {
        return complaintService.getStudentComplaints(getCurrentUser().getId(), checked(pageable));
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
