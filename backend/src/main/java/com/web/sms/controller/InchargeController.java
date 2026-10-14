package com.web.sms.controller;

import com.web.sms.dto.response.ApplicationResponse;
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
import com.web.sms.enums.PaymentStatus;
import com.web.sms.repository.AcademicYearRepository;
import com.web.sms.repository.BusApplicationRepository;
import com.web.sms.repository.BusPassRepository;
import com.web.sms.repository.FeeRepository;
import com.web.sms.repository.TransportAllocationRepository;
import com.web.sms.repository.TransferRequestRepository;
import com.web.sms.repository.PaymentRepository;
import com.web.sms.repository.BusInchargeAssignmentRepository;
import com.web.sms.repository.StudentAcademicDetailsRepository;
import com.web.sms.exception.ForbiddenException;
import com.web.sms.exception.ResourceNotFoundException;
import com.web.sms.exception.BadRequestException;
import com.web.sms.security.UserPrincipal;
import com.web.sms.service.ApplicationService;
import com.web.sms.service.InchargeService;
import com.web.sms.service.TransferService;
import com.web.sms.service.InchargeOperationsService;
import com.web.sms.service.NotificationService;
import com.web.sms.service.TransportReportService;
import com.web.sms.service.PassService;
import com.web.sms.service.AdminService;
import com.web.sms.dto.request.UpdateStudentRequest;
import com.web.sms.dto.request.TransportReportFilter;
import com.web.sms.dto.response.StudentProfileResponse;
import com.web.sms.dto.response.PassResponse;
import com.web.sms.dto.response.BusResponse;
import com.web.sms.dto.response.NotificationResponse;
import com.web.sms.dto.response.InchargeStudentResponse;
import com.web.sms.dto.response.PaymentResponse;
import com.web.sms.dto.response.StudentYearHistoryResponse;
import com.web.sms.dto.request.PaymentRequest;
import com.web.sms.dto.request.RazorpayVerificationRequest;
import com.web.sms.dto.response.RazorpayOrderResponse;
import com.web.sms.service.StudentHistoryService;
import com.web.sms.service.RazorpayPaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/incharge")
public class InchargeController {

    private final InchargeService inchargeService;
    private final ApplicationService applicationService;
    private final TransferService transferService;
    private final TransportAllocationRepository allocationRepo;
    private final BusApplicationRepository applicationRepo;
    private final FeeRepository feeRepo;
    private final BusPassRepository passRepo;
    private final AcademicYearRepository academicYearRepo;
    private final TransferRequestRepository transferRepo;
    private final InchargeOperationsService operations;
    private final NotificationService notificationService;
    private final TransportReportService reportService;
    private final PassService passService;
    private final AdminService adminService;
    private final PaymentRepository paymentRepo;
    private final BusInchargeAssignmentRepository busInchargeAssignmentRepo;
    private final StudentAcademicDetailsRepository studentAcademicDetailsRepo;
    private final StudentHistoryService studentHistoryService;
    private final RazorpayPaymentService razorpayPaymentService;

    public InchargeController(InchargeService inchargeService,
                              ApplicationService applicationService,
                              TransferService transferService,
                              TransportAllocationRepository allocationRepo,
                              BusApplicationRepository applicationRepo,
                              FeeRepository feeRepo,
                              BusPassRepository passRepo,
                              AcademicYearRepository academicYearRepo,
                              TransferRequestRepository transferRepo, InchargeOperationsService operations,
                              NotificationService notificationService, TransportReportService reportService,
                              PassService passService,AdminService adminService, PaymentRepository paymentRepo,
                              BusInchargeAssignmentRepository busInchargeAssignmentRepo,
                              StudentAcademicDetailsRepository studentAcademicDetailsRepo,
                              StudentHistoryService studentHistoryService,
                              RazorpayPaymentService razorpayPaymentService) {
        this.inchargeService = inchargeService;
        this.applicationService = applicationService;
        this.transferService = transferService;
        this.allocationRepo = allocationRepo;
        this.applicationRepo = applicationRepo;
        this.feeRepo = feeRepo;
        this.passRepo = passRepo;
        this.academicYearRepo = academicYearRepo;
        this.transferRepo = transferRepo;
        this.operations = operations;
        this.notificationService = notificationService;
        this.reportService = reportService;
        this.passService = passService;
        this.adminService=adminService;
        this.paymentRepo=paymentRepo;
        this.busInchargeAssignmentRepo=busInchargeAssignmentRepo;
        this.studentAcademicDetailsRepo=studentAcademicDetailsRepo;
        this.studentHistoryService=studentHistoryService;
        this.razorpayPaymentService=razorpayPaymentService;
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

    private Bus requireAssignedBus(Long academicYearId) {
        if (academicYearId == null) return requireAssignedBus();
        return busInchargeAssignmentRepo.findFirstByInchargeIdAndAcademicYearIdOrderByAssignedAtDesc(getCurrentUser().getId(), academicYearId)
                .map(com.web.sms.entity.BusInchargeAssignment::getBus)
                .orElseThrow(() -> new ForbiddenException("You were not assigned to a bus in the selected academic year"));
    }

    private Pageable requestedPage(boolean unpaged, Pageable pageable) { return unpaged ? Pageable.unpaged() : checked(pageable); }
    private Pageable checked(Pageable pageable) {
        if (pageable.getPageNumber() < 0 || pageable.getPageSize() < 1 || pageable.getPageSize() > 6000) {
            throw new com.web.sms.exception.BadRequestException("Page size must be between 1 and 6000");
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
    public Map<String,Object> profile(){com.web.sms.entity.Incharge i=inchargeService.getInchargeById(getCurrentUser().getId());Map<String,Object> r=new LinkedHashMap<>();r.put("teacherId",i.getTeacherId());r.put("name",i.getName());r.put("email",i.getEmail());r.put("phoneNumber",i.getPhoneNumber());r.put("address",i.getAddress());r.put("department",i.getDepartment());r.put("designation",i.getDesignation());r.put("photoAvailable",i.getPhotoData()!=null&&i.getPhotoData().length>0);Bus b=getAssignedBus();r.put("assignedBus",b==null?null:b.getBusNumber());return r;}
    @GetMapping("/profile/photo") public ResponseEntity<byte[]> profilePhoto(){com.web.sms.entity.Incharge i=inchargeService.getInchargeById(getCurrentUser().getId());if(i.getPhotoData()==null||i.getPhotoData().length==0)throw new ResourceNotFoundException("Profile photo not found");return ResponseEntity.ok().contentType(MediaType.parseMediaType(i.getPhotoContentType()==null?MediaType.APPLICATION_OCTET_STREAM_VALUE:i.getPhotoContentType())).body(i.getPhotoData());}

    @GetMapping("/fees")
    public List<com.web.sms.dto.response.FeeResponse> fees(@RequestParam(required = false) Long academicYearId) {
        return feeRepo.findForIncharge(requireAssignedBus(academicYearId).getId(), academicYearId)
                .stream().map(com.web.sms.dto.response.FeeResponse::fromFee).toList();
    }

    @PostMapping("/payments/order")
    public RazorpayOrderResponse createPaymentOrder(@Valid @RequestBody PaymentRequest req) {
        return razorpayPaymentService.createStaffOrder(getCurrentUser().getId(), getCurrentUser().getRole(), req);
    }

    @PostMapping("/payments/verify")
    public PaymentResponse verifyPayment(@Valid @RequestBody RazorpayVerificationRequest req) {
        return razorpayPaymentService.verifyStaffCheckout(getCurrentUser().getId(), getCurrentUser().getRole(), req);
    }

    @GetMapping("/passes")
    public List<PassResponse> passes(@RequestParam(required = false) Long academicYearId) {
        return passRepo.findForIncharge(requireAssignedBus(academicYearId).getId(), academicYearId)
                .stream().map(PassResponse::fromBusPass).toList();
    }
    @GetMapping("/passes/{id}") public PassResponse pass(@PathVariable Long id){return passService.getPassForIncharge(id,requireAssignedBus().getId());}
    @GetMapping("/passes/{id}/photo") public ResponseEntity<byte[]> passPhoto(@PathVariable Long id){return passService.passPhotoForIncharge(id,requireAssignedBus().getId());}
    @GetMapping("/passes/{id}/download") public ResponseEntity<byte[]> downloadPass(@PathVariable Long id){PassResponse pass=passService.getPassForIncharge(id,requireAssignedBus().getId());return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=bus-pass-"+pass.getRollNumber()+".pdf").body(passService.generatePdfForIncharge(id,requireAssignedBus().getId()));}

    @GetMapping("/reports")
    public DashboardResponse reports(){return operations.dashboard(requireAssignedBus());}
    @GetMapping("/reports/students") public Page<TransportReportRow> studentReport(@ModelAttribute com.web.sms.dto.request.TransportReportFilter filter,@RequestParam(defaultValue="false") boolean unpaged,Pageable pageable){return reportService.report(requireAssignedBus(filter.getAcademicYearId()).getId(),filter,requestedPage(unpaged,pageable));}
    @GetMapping("/reports/students/export/{format}") public ResponseEntity<byte[]> exportStudentReport(@PathVariable String format,@ModelAttribute com.web.sms.dto.request.TransportReportFilter filter){Bus bus=requireAssignedBus(filter.getAcademicYearId());boolean excel="excel".equalsIgnoreCase(format)||"xlsx".equalsIgnoreCase(format);boolean pdf="pdf".equalsIgnoreCase(format);if(!excel&&!pdf)throw new com.web.sms.exception.BadRequestException("Export format must be excel or pdf");byte[] body=excel?reportService.excel(bus.getId(),filter,"Transport Report - "+bus.getBusNumber()):reportService.pdf(bus.getId(),filter,"Transport Report - "+bus.getBusNumber());String ext=excel?"xlsx":"pdf";MediaType type=excel?MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"):MediaType.APPLICATION_PDF;return ResponseEntity.ok().contentType(type).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename="+bus.getBusNumber()+"-transport-report."+ext).body(body);}

    @GetMapping("/notifications")
    public List<com.web.sms.dto.response.NotificationResponse> notifications() {
        return notificationService.getNotifications(getCurrentUser().getUsername(), Pageable.unpaged()).getContent();
    }
    @PutMapping("/notifications/{id}/read") public void markNotificationRead(@PathVariable Long id){notificationService.markAsRead(id,getCurrentUser().getUsername());}

    @GetMapping("/applications")
    public Page<ApplicationResponse> getApplications(@RequestParam(required = false) ApplicationStatus status, @RequestParam(required = false) Long academicYearId, @RequestParam(defaultValue="false") boolean unpaged, Pageable pageable) {
        Bus bus = academicYearId == null ? getAssignedBus() : requireAssignedBus(academicYearId);
        if (bus == null) return Page.empty();
        return applicationService.getApplicationsByBus(bus.getId(), status, academicYearId, requestedPage(unpaged,pageable));
    }
    @GetMapping("/applications/summary")
    public Map<String,Long> applicationSummary(@RequestParam(required = false) Long academicYearId) {
        Bus bus = requireAssignedBus(academicYearId);
        Map<String,Long> result = new LinkedHashMap<>();
        List<BusApplication> apps = applicationRepo.findByBusId(bus.getId(), Pageable.unpaged()).getContent().stream()
                .filter(a -> academicYearId == null || (a.getAcademicYear() != null && a.getAcademicYear().getId().equals(academicYearId)))
                .toList();
        result.put("total", (long) apps.size());
        for (ApplicationStatus status : ApplicationStatus.values())
            result.put(status.name().toLowerCase(), apps.stream().filter(a -> a.getStatus() == status).count());
        return result;
    }

    @PutMapping("/applications/{id}/approve")
    public ApplicationResponse approveApplication(@PathVariable Long id) {
        Bus bus=requireAssignedBus();
        return applicationService.approveApplication(id, getCurrentUser().getUsername(), bus.getId());
    }

    @PutMapping("/applications/{id}/reject")
    public ApplicationResponse rejectApplication(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        Bus bus=requireAssignedBus();
        String remarks = (body != null) ? body.getOrDefault("remarks", "").trim() : "";
        String finalRemarks = remarks.isEmpty() ? "Rejected by Incharge" : remarks;
        return applicationService.rejectApplication(id, finalRemarks, getCurrentUser().getUsername(), bus.getId());
    }

    @GetMapping("/applications/{id}/student-details")
    public StudentProfileResponse getApplicationStudentDetails(@PathVariable Long id) {
        requireApplicationAccess(id);
        BusApplication app = applicationRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        return studentAcademicDetailsRepo.findByStudentIdAndAcademicYearId(app.getStudent().getId(), app.getAcademicYear().getId())
                .map(details -> StudentProfileResponse.fromStudent(app.getStudent(), details))
                .orElseGet(() -> StudentProfileResponse.fromStudent(app.getStudent()));
    }

    @GetMapping("/applications/{id}/student-photo")
    public ResponseEntity<byte[]> getApplicationStudentPhoto(@PathVariable Long id) {
        requireApplicationAccess(id);
        BusApplication app = applicationRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        Student st = app.getStudent();
        if (st.getPhotoData() == null || st.getPhotoData().length == 0) throw new ResourceNotFoundException("Student photo not found");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(st.getPhotoContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : st.getPhotoContentType())).body(st.getPhotoData());
    }

    @GetMapping("/students")
    public Page<InchargeStudentResponse> getStudents(@RequestParam(required=false) String search,@RequestParam(required=false) String branch,@RequestParam(required=false) Integer year,@RequestParam(required=false) Long boardingPointId,@RequestParam(required=false) String paymentStatus,@RequestParam(required=false) String passStatus,@RequestParam(required=false) Long academicYearId,@RequestParam(defaultValue="false") boolean unpaged,Pageable pageable) {
        return operations.students(requireAssignedBus(academicYearId),search,branch,year,boardingPointId,paymentStatus,passStatus,academicYearId,requestedPage(unpaged,pageable));
    }
    @PutMapping(value="/students/{studentId}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public StudentProfileResponse updateStudent(@PathVariable Long studentId,@Valid @RequestPart("student") UpdateStudentRequest req,@RequestPart(value="photo",required=false) org.springframework.web.multipart.MultipartFile photo){return adminService.updateStudentForIncharge(studentId,requireAssignedBus().getId(),req,photo,getCurrentUser().getUsername());}

    @GetMapping("/transfers")
    public Page<TransferResponse> getTransfers(@RequestParam(defaultValue="false") boolean unpaged, Pageable pageable) {
        Bus bus = getAssignedBus();
        if (bus == null) return Page.empty();
        return transferService.getTransfersByBus(bus.getId(), requestedPage(unpaged,pageable));
    }

    @GetMapping("/transfers/pending-release")
    public Page<TransferResponse> pendingRelease(@RequestParam(defaultValue="false") boolean unpaged, Pageable pageable) {
        return transferService.getPendingRelease(requireAssignedBus().getId(), requestedPage(unpaged,pageable));
    }

    @GetMapping("/transfers/pending-acceptance")
    public Page<TransferResponse> pendingAcceptance(@RequestParam(defaultValue="false") boolean unpaged, Pageable pageable) {
        return transferService.getPendingAcceptance(requireAssignedBus().getId(), requestedPage(unpaged,pageable));
    }

    @PostMapping("/transfers/{id}/approve-release")
    public TransferResponse approveRelease(@PathVariable Long id) {
        requireTransferAccess(id);
        return transferService.approveRelease(id, getCurrentUser().getId(), getCurrentUser().getUsername());
    }

    @PostMapping("/transfers/{id}/reject-release")
    public TransferResponse rejectRelease(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        requireTransferAccess(id);
        String remarks = (body != null) ? body.getOrDefault("remarks", "").trim() : "";
        String finalRemarks = remarks.isEmpty() ? "Rejected by current In-Charge" : remarks;
        return transferService.rejectRelease(id, getCurrentUser().getId(), getCurrentUser().getUsername(), finalRemarks);
    }

    @PostMapping("/transfers/{id}/accept")
    public TransferResponse acceptTransfer(@PathVariable Long id) {
        requireTransferAccess(id);
        return transferService.acceptTransfer(id, getCurrentUser().getId(), getCurrentUser().getUsername());
    }

    @PostMapping("/transfers/{id}/reject")
    public TransferResponse rejectAcceptance(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        requireTransferAccess(id);
        String remarks = (body != null) ? body.getOrDefault("remarks", "").trim() : "";
        String finalRemarks = remarks.isEmpty() ? "Rejected by new In-Charge" : remarks;
        return transferService.rejectAcceptance(id, getCurrentUser().getId(), getCurrentUser().getUsername(), finalRemarks);
    }

    @GetMapping("/buses")
    public List<BusResponse> getBuses() {
        Bus b = getAssignedBus();
        return b == null ? List.of() : List.of(operations.bus(b));
    }

    @GetMapping("/academic-years")
    public List<com.web.sms.entity.AcademicYear> academicYears() {
        return academicYearRepo.findAll();
    }

    @GetMapping("/payments")
    public List<PaymentResponse> payments(@RequestParam(required = false) Long academicYearId,
                                          @RequestParam(required = false) LocalDate fromDate,
                                          @RequestParam(required = false) LocalDate toDate,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String mode,
                                          @RequestParam(required = false) String search) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("From date must not be after To date");
        }
        PaymentStatus paymentStatus = null;
        if (status != null && !status.isBlank()) {
            try { paymentStatus = PaymentStatus.valueOf(status.trim().toUpperCase()); }
            catch (IllegalArgumentException exception) { throw new BadRequestException("Invalid payment status"); }
        }
        String normalizedMode = mode == null || mode.isBlank() ? null : mode.trim().toUpperCase();
        if (normalizedMode != null && !List.of("RAZORPAY", "ADMIN_MANUAL").contains(normalizedMode)) {
            throw new BadRequestException("Invalid payment mode");
        }
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        LocalDateTime from = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime toExclusive = toDate == null ? null : toDate.plusDays(1).atStartOfDay();
        Bus bus = academicYearId == null ? requireAssignedBus() : requireAssignedBus(academicYearId);
        return paymentRepo.findForInchargeReport(bus.getId(), academicYearId, from, toExclusive,
                        paymentStatus, normalizedMode, normalizedSearch).stream()
                .map(PaymentResponse::fromPayment)
                .toList();
    }

    @GetMapping("/students/{studentId}/photo")
    public ResponseEntity<byte[]> getStudentPhoto(@PathVariable Long studentId) {
        var allowed=authorizedStudentHistory(studentId);
        if(allowed.isEmpty())throw new ForbiddenException("This student was not assigned to a bus you managed");
        TransportAllocation allocation = allocationRepo.findByStudentId(studentId).stream().findFirst().orElseThrow(()->new ResourceNotFoundException("Student allocation not found"));
        Student st = allocation.getStudent();
        if (st.getPhotoData() == null || st.getPhotoData().length == 0) throw new ResourceNotFoundException("Student photo not found");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(st.getPhotoContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : st.getPhotoContentType()))
                .body(st.getPhotoData());
    }

    @GetMapping("/students/{studentId}/history")
    public List<StudentYearHistoryResponse> studentHistory(@PathVariable Long studentId){return authorizedStudentHistory(studentId);}

    private List<StudentYearHistoryResponse> authorizedStudentHistory(Long studentId){
        var assignments=busInchargeAssignmentRepo.findByInchargeId(getCurrentUser().getId());
        java.util.Set<String> scopes=assignments.stream().map(a->a.getBus().getId()+":"+a.getAcademicYear().getId()).collect(java.util.stream.Collectors.toSet());
        java.util.Set<Long> allowedYears=allocationRepo.findByStudentId(studentId).stream()
                .filter(a->a.getBus()!=null&&a.getAcademicYear()!=null&&scopes.contains(a.getBus().getId()+":"+a.getAcademicYear().getId()))
                .map(a->a.getAcademicYear().getId()).collect(java.util.stream.Collectors.toSet());
        if(allowedYears.isEmpty())throw new ForbiddenException("This student was not assigned to a bus you managed");
        return studentHistoryService.history(studentId,allowedYears);
    }

}
