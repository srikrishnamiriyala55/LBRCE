package com.web.sms.controller;

import com.web.sms.dto.response.ApplicationResponse;
import com.web.sms.dto.response.ComplaintResponse;
import com.web.sms.dto.response.DashboardResponse;
import com.web.sms.dto.response.TransferResponse;
import com.web.sms.dto.response.TransportReportRow;
import com.web.sms.entity.AcademicYear;
import com.web.sms.entity.Bus;
import com.web.sms.entity.BusPass;
import com.web.sms.entity.Fee;
import com.web.sms.entity.Student;
import com.web.sms.entity.TransportAllocation;
import com.web.sms.entity.BusApplication;
import com.web.sms.entity.TransferRequest;
import com.web.sms.enums.ApplicationStatus;
import com.web.sms.enums.EntityStatus;
import com.web.sms.enums.PassStatus;
import com.web.sms.repository.AcademicYearRepository;
import com.web.sms.repository.BusApplicationRepository;
import com.web.sms.repository.BusPassRepository;
import com.web.sms.repository.FeeRepository;
import com.web.sms.repository.TransportAllocationRepository;
import com.web.sms.repository.TransferRequestRepository;
import com.web.sms.exception.ForbiddenException;
import com.web.sms.exception.ResourceNotFoundException;
import com.web.sms.security.UserPrincipal;
import com.web.sms.service.ApplicationService;
import com.web.sms.service.ComplaintService;
import com.web.sms.service.InchargeService;
import com.web.sms.service.TransferService;
import com.web.sms.service.InchargeOperationsService;
import com.web.sms.service.NotificationService;
import com.web.sms.service.TransportReportService;
import com.web.sms.service.PassService;
import com.web.sms.dto.response.PassResponse;
import com.web.sms.dto.response.InchargeStudentResponse;
import com.web.sms.repository.ComplaintRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/incharge")
public class InchargeController {

    private final InchargeService inchargeService;
    private final ApplicationService applicationService;
    private final TransferService transferService;
    private final ComplaintService complaintService;
    private final TransportAllocationRepository allocationRepo;
    private final BusApplicationRepository applicationRepo;
    private final FeeRepository feeRepo;
    private final BusPassRepository passRepo;
    private final AcademicYearRepository academicYearRepo;
    private final TransferRequestRepository transferRepo;
    private final InchargeOperationsService operations;
    private final ComplaintRepository complaintRepo;
    private final NotificationService notificationService;
    private final TransportReportService reportService;
    private final PassService passService;

    public InchargeController(InchargeService inchargeService,
                              ApplicationService applicationService,
                              TransferService transferService,
                              ComplaintService complaintService,
                              TransportAllocationRepository allocationRepo,
                              BusApplicationRepository applicationRepo,
                              FeeRepository feeRepo,
                              BusPassRepository passRepo,
                              AcademicYearRepository academicYearRepo,
                              TransferRequestRepository transferRepo, InchargeOperationsService operations,
                              ComplaintRepository complaintRepo, NotificationService notificationService, TransportReportService reportService,
                              PassService passService) {
        this.inchargeService = inchargeService;
        this.applicationService = applicationService;
        this.transferService = transferService;
        this.complaintService = complaintService;
        this.allocationRepo = allocationRepo;
        this.applicationRepo = applicationRepo;
        this.feeRepo = feeRepo;
        this.passRepo = passRepo;
        this.academicYearRepo = academicYearRepo;
        this.transferRepo = transferRepo;
        this.operations = operations;
        this.complaintRepo = complaintRepo;
        this.notificationService = notificationService;
        this.reportService = reportService;
        this.passService = passService;
    }

    private UserPrincipal getCurrentUser() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private Bus getAssignedBus() {
        return inchargeService.getAssignedBus(getCurrentUser().getId());
    }

    private Bus requireAssignedBus() {
        Bus bus = getAssignedBus();
        if (bus == null) throw new ForbiddenException("No bus is assigned to this incharge");
        if (bus.getStatus() != EntityStatus.ACTIVE) throw new ForbiddenException("Your assigned bus is inactive");
        return bus;
    }

    private Pageable checked(Pageable pageable) {
        if (pageable.getPageNumber() < 0 || pageable.getPageSize() < 1 || pageable.getPageSize() > 50) {
            throw new com.web.sms.exception.BadRequestException("Page size must be between 1 and 50");
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
    }

    private void requireApplicationAccess(Long applicationId) {
        BusApplication application = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        if (!application.getBus().getId().equals(requireAssignedBus().getId())) {
            throw new ForbiddenException("This application does not belong to your assigned bus");
        }
    }

    private void requireTransferAccess(Long transferId) {
        TransferRequest transfer = transferRepo.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer request not found"));
        Long assignedBusId = requireAssignedBus().getId();
        if (!transfer.getCurrentBus().getId().equals(assignedBusId)
                && !transfer.getRequestedBus().getId().equals(assignedBusId)) {
            throw new ForbiddenException("This transfer does not involve your assigned bus");
        }
    }

    @GetMapping("/dashboard")
    public DashboardResponse getDashboard() {
        Bus bus = getAssignedBus();
        if (bus == null) {
            DashboardResponse res = new DashboardResponse();
            res.put("busNumber", "Not Assigned");
            res.put("startingPoint", "N/A");
            res.put("endingPoint", "N/A");
            res.put("totalCapacity", 0);
            res.put("totalStudents", 0);
            res.put("pendingApplications", 0);
            res.put("vacancy", 0);
            return res;
        }
        return operations.dashboard(bus);
    }

    @GetMapping("/assigned-bus")
    public com.web.sms.dto.response.BusResponse assignedBus() { return operations.bus(requireAssignedBus()); }

    @GetMapping("/profile")
    public Map<String,Object> profile(){com.web.sms.entity.Incharge i=inchargeService.getInchargeById(getCurrentUser().getId());Map<String,Object> r=new LinkedHashMap<>();r.put("teacherId",i.getTeacherId());r.put("name",i.getName());r.put("email",i.getEmail());r.put("phoneNumber",i.getPhoneNumber());r.put("department",i.getDepartment());r.put("designation",i.getDesignation());Bus b=getAssignedBus();r.put("assignedBus",b==null?null:b.getBusNumber());return r;}

    @GetMapping("/fees")
    public Page<com.web.sms.dto.response.FeeResponse> fees(Pageable pageable){return feeRepo.findByAllocationBusIdAndAllocationStatus(requireAssignedBus().getId(),EntityStatus.ACTIVE,checked(pageable)).map(com.web.sms.dto.response.FeeResponse::fromFee);}

    @GetMapping("/passes")
    public Page<PassResponse> passes(Pageable pageable){return passRepo.findByAllocationBusIdAndAllocationStatus(requireAssignedBus().getId(),EntityStatus.ACTIVE,checked(pageable)).map(PassResponse::fromBusPass);}
    @GetMapping("/passes/{id}") public PassResponse pass(@PathVariable Long id){return passService.getPassForIncharge(id,requireAssignedBus().getId());}
    @GetMapping("/passes/{id}/photo") public ResponseEntity<byte[]> passPhoto(@PathVariable Long id){return passService.passPhotoForIncharge(id,requireAssignedBus().getId());}
    @GetMapping("/passes/{id}/download") public ResponseEntity<byte[]> downloadPass(@PathVariable Long id){PassResponse pass=passService.getPassForIncharge(id,requireAssignedBus().getId());return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=bus-pass-"+pass.getRollNumber()+".pdf").body(passService.generatePdfForIncharge(id,requireAssignedBus().getId()));}

    @GetMapping("/reports")
    public DashboardResponse reports(){return operations.dashboard(requireAssignedBus());}
    @GetMapping("/reports/students") public Page<TransportReportRow> studentReport(@ModelAttribute com.web.sms.dto.request.TransportReportFilter filter,Pageable pageable){return reportService.report(requireAssignedBus().getId(),filter,checked(pageable));}
    @GetMapping("/reports/students/export/{format}") public ResponseEntity<byte[]> exportStudentReport(@PathVariable String format,@ModelAttribute com.web.sms.dto.request.TransportReportFilter filter){Bus bus=requireAssignedBus();boolean excel="excel".equalsIgnoreCase(format)||"xlsx".equalsIgnoreCase(format);boolean pdf="pdf".equalsIgnoreCase(format);if(!excel&&!pdf)throw new com.web.sms.exception.BadRequestException("Export format must be excel or pdf");byte[] body=excel?reportService.excel(bus.getId(),filter,"Transport Report - "+bus.getBusNumber()):reportService.pdf(bus.getId(),filter,"Transport Report - "+bus.getBusNumber());String ext=excel?"xlsx":"pdf";MediaType type=excel?MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"):MediaType.APPLICATION_PDF;return ResponseEntity.ok().contentType(type).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename="+bus.getBusNumber()+"-transport-report."+ext).body(body);}

    @GetMapping("/notifications")
    public Page<com.web.sms.dto.response.NotificationResponse> notifications(Pageable pageable){return notificationService.getNotifications(getCurrentUser().getUsername(),checked(pageable));}
    @PutMapping("/notifications/{id}/read") public void markNotificationRead(@PathVariable Long id){notificationService.markAsRead(id,getCurrentUser().getUsername());}

    @GetMapping("/applications")
    public Page<ApplicationResponse> getApplications(@RequestParam(required = false) ApplicationStatus status, Pageable pageable) {
        Bus bus = getAssignedBus();
        if (bus == null) return Page.empty();
        return applicationService.getApplicationsByBus(bus.getId(), status, checked(pageable));
    }

    @PutMapping("/applications/{id}/approve")
    public ApplicationResponse approveApplication(@PathVariable Long id) {
        Bus bus=requireAssignedBus();
        return applicationService.approveApplication(id, getCurrentUser().getUsername(), bus.getId());
    }

    @PutMapping("/applications/{id}/reject")
    public ApplicationResponse rejectApplication(@PathVariable Long id, @RequestBody(required = false) String remarks) {
        Bus bus=requireAssignedBus();
        String cleanRemarks = parseRemarks(remarks);
        return applicationService.rejectApplication(id, cleanRemarks != null ? cleanRemarks : "Rejected by Incharge", getCurrentUser().getUsername(), bus.getId());
    }

    @GetMapping("/students")
    public Page<InchargeStudentResponse> getStudents(@RequestParam(required=false) String search,@RequestParam(required=false) String branch,@RequestParam(required=false) Integer year,@RequestParam(required=false) Long boardingPointId,@RequestParam(required=false) String paymentStatus,@RequestParam(required=false) String passStatus,Pageable pageable) {
        return operations.students(requireAssignedBus(),search,branch,year,boardingPointId,paymentStatus,passStatus,checked(pageable));
    }

    @GetMapping("/transfers")
    public Page<TransferResponse> getTransfers(Pageable pageable) {
        Bus bus = getAssignedBus();
        if (bus == null) return Page.empty();
        return transferService.getTransfersByBus(bus.getId(), checked(pageable));
    }

    @GetMapping("/transfers/pending-release")
    public Page<TransferResponse> pendingRelease(Pageable pageable) {
        return transferService.getPendingRelease(requireAssignedBus().getId(), checked(pageable));
    }

    @GetMapping("/transfers/pending-acceptance")
    public Page<TransferResponse> pendingAcceptance(Pageable pageable) {
        return transferService.getPendingAcceptance(requireAssignedBus().getId(), checked(pageable));
    }

    @PostMapping("/transfers/{id}/approve-release")
    public TransferResponse approveRelease(@PathVariable Long id) {
        requireTransferAccess(id);
        return transferService.approveRelease(id, getCurrentUser().getId(), getCurrentUser().getUsername());
    }

    @PostMapping("/transfers/{id}/reject-release")
    public TransferResponse rejectRelease(@PathVariable Long id, @RequestBody(required = false) String remarks) {
        requireTransferAccess(id);
        String cleanRemarks = parseRemarks(remarks);
        return transferService.rejectRelease(id, getCurrentUser().getId(), getCurrentUser().getUsername(), cleanRemarks != null ? cleanRemarks : "Rejected by current In-Charge");
    }

    @PostMapping("/transfers/{id}/accept")
    public TransferResponse acceptTransfer(@PathVariable Long id) {
        requireTransferAccess(id);
        return transferService.acceptTransfer(id, getCurrentUser().getId(), getCurrentUser().getUsername());
    }

    @PostMapping("/transfers/{id}/reject")
    public TransferResponse rejectAcceptance(@PathVariable Long id, @RequestBody(required = false) String remarks) {
        requireTransferAccess(id);
        String cleanRemarks = parseRemarks(remarks);
        return transferService.rejectAcceptance(id, getCurrentUser().getId(), getCurrentUser().getUsername(), cleanRemarks != null ? cleanRemarks : "Rejected by new In-Charge");
    }

    private String parseRemarks(String raw) {
        if (raw == null || raw.isBlank()) return null;
        if (raw.contains("\"remarks\"")) {
            try {
                int idx = raw.indexOf("\"remarks\"");
                int start = raw.indexOf(":", idx);
                if (start != -1) {
                    String sub = raw.substring(start + 1).trim();
                    if (sub.startsWith("\"")) {
                        int end = sub.indexOf("\"", 1);
                        if (end != -1) return sub.substring(1, end);
                    }
                }
            } catch (Exception ignored) {}
        }
        return raw.trim();
    }

    @GetMapping("/complaints")
    public Page<ComplaintResponse> getComplaints(Pageable pageable) {
        Bus bus = getAssignedBus();
        if (bus == null) return Page.empty();
        return complaintService.getComplaintsByBus(bus.getId(), checked(pageable));
    }

    @PutMapping("/complaints/{id}/status")
    public ComplaintResponse updateComplaintStatus(@PathVariable Long id, @RequestBody ComplaintResponse req) {
        Bus bus=requireAssignedBus();
        return complaintService.updateComplaintStatus(id, req.getStatus() != null ? req.getStatus().name() : "RESOLVED", req.getResponse(), getCurrentUser().getUsername(),bus.getId());
    }
}
