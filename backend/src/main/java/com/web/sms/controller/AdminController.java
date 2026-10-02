package com.web.sms.controller;

import com.web.sms.dto.request.*;
import com.web.sms.dto.response.*;
import com.web.sms.entity.*;
import com.web.sms.enums.ApplicationStatus;
import com.web.sms.enums.EntityStatus;
import com.web.sms.enums.TransferStatus;
import com.web.sms.exception.ResourceNotFoundException;
import com.web.sms.repository.*;
import com.web.sms.security.UserPrincipal;
import com.web.sms.service.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final BusService busService;
    private final InchargeService inchargeService;
    private final ApplicationService applicationService;
    private final TransferService transferService;
    private final StudentRepository studentRepo;
    private final BusRepository busRepo;
    private final BoardingPointsRepository boardingPointsRepo;
    private final BusApplicationRepository applicationRepo;
    private final FeeRepository feeRepo;
    private final TransferRequestRepository transferRepo;
    private final AcademicYearRepository academicYearRepo;
    private final AuditService auditService;
    private final PaymentRepository paymentRepo;
    private final BusPassRepository passRepo;
    private final NotificationRepository notificationRepo;
    private final AdminRepository adminRepo;
    private final TransportAllocationRepository allocationRepo;
    private final InchargeRepository inchargeRepo;
    private final TransportReportService reportService;
    private final PassService passService;
    private final FeeService feeService;

    public AdminController(AdminService adminService,
                           BusService busService,
                           InchargeService inchargeService,
                           ApplicationService applicationService,
                           TransferService transferService,
                           StudentRepository studentRepo,
                           BusRepository busRepo,
                           BoardingPointsRepository boardingPointsRepo,
                           BusApplicationRepository applicationRepo,
                           FeeRepository feeRepo,
                           TransferRequestRepository transferRepo,
                           AcademicYearRepository academicYearRepo,
                           AuditService auditService, PaymentRepository paymentRepo,
                           BusPassRepository passRepo,
                           NotificationRepository notificationRepo, AdminRepository adminRepo,
                           TransportAllocationRepository allocationRepo,
                           InchargeRepository inchargeRepo, TransportReportService reportService,
                           PassService passService, FeeService feeService) {
        this.adminService = adminService;
        this.busService = busService;
        this.inchargeService = inchargeService;
        this.applicationService = applicationService;
        this.transferService = transferService;
        this.studentRepo = studentRepo;
        this.busRepo = busRepo;
        this.boardingPointsRepo = boardingPointsRepo;
        this.applicationRepo = applicationRepo;
        this.feeRepo = feeRepo;
        this.transferRepo = transferRepo;
        this.academicYearRepo = academicYearRepo;
        this.auditService = auditService;
        this.paymentRepo=paymentRepo;this.passRepo=passRepo;this.notificationRepo=notificationRepo;this.adminRepo=adminRepo;
        this.allocationRepo=allocationRepo;
        this.inchargeRepo=inchargeRepo;
        this.reportService=reportService;
        this.passService=passService;
        this.feeService=feeService;
    }

    private UserPrincipal getCurrentUser() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping("/dashboard")
    public DashboardResponse getDashboard() {
        return adminService.getDashboard();
    }

    @GetMapping("/buses")
    public List<BusResponse> getBuses() {
        return busRepo.findAll().stream().map(b->BusResponse.fromBus(b,(int)allocationRepo.countByBusIdAndStatus(b.getId(),EntityStatus.ACTIVE))).toList();
    }

    @PostMapping("/buses")
    public BusResponse createBus(@Valid @RequestBody CreateBusRequest req) {
        BusResponse r=busService.createBus(req);auditService.log(getCurrentUser().getUsername(),"ADMIN","BUS_CREATED","BUS",String.valueOf(r.getId()),null,r.getBusNumber(),null);return r;
    }

    @PutMapping("/buses/{id}")
    public BusResponse updateBus(@PathVariable Long id, @Valid @RequestBody CreateBusRequest req) {
        BusResponse r=busService.updateBus(id, req);auditService.log(getCurrentUser().getUsername(),"ADMIN","BUS_UPDATED","BUS",String.valueOf(id),null,r.getBusNumber(),null);return r;
    }

    @DeleteMapping("/buses/{id}")
    public void deleteBus(@PathVariable Long id) {
        adminService.deactivateBus(id,getCurrentUser().getUsername());
    }

    @PutMapping("/buses/{id}/incharge")
    public BusResponse assignIncharge(@PathVariable Long id,@Valid @RequestBody AssignInchargeRequest req){return adminService.assignIncharge(id,req.getInchargeId(),getCurrentUser().getUsername());}
    @DeleteMapping("/buses/{id}/incharge")
    public BusResponse unassignIncharge(@PathVariable Long id){return adminService.unassignIncharge(id,getCurrentUser().getUsername());}

    @PutMapping("/boarding-points/{id}/status") @Transactional public BoardingPointResponse boardingStatus(@PathVariable Long id,@Valid @RequestBody StatusUpdateRequest req){BoardingPoints p=boardingPointsRepo.findById(id).orElseThrow(()->new ResourceNotFoundException("Boarding point not found"));EntityStatus next=parseEntityStatus(req.getStatus());if(next!=EntityStatus.ACTIVE&&allocationRepo.countByBoardingPointIdAndStatus(id,EntityStatus.ACTIVE)>0)throw new com.web.sms.exception.BadRequestException("Cannot deactivate a boarding point used by active students");p.setStatus(next);boardingPointsRepo.save(p);auditService.log(getCurrentUser().getUsername(),"ADMIN","BOARDING_POINT_STATUS_UPDATED","BOARDING_POINT",String.valueOf(id),null,next.name(),null);return BoardingPointResponse.fromBoardingPoint(p);}

    @PutMapping("/incharges/{id}/status") @Transactional public Map<String,Object> inchargeStatus(@PathVariable Long id,@Valid @RequestBody StatusUpdateRequest req){Incharge i=inchargeService.getInchargeById(id);String next=req.getStatus().toUpperCase();if(!java.util.Set.of("ACTIVE","INACTIVE").contains(next))throw new com.web.sms.exception.BadRequestException("Status must be ACTIVE or INACTIVE");if("INACTIVE".equals(next)&&!busRepo.findByInchargeId(id).isEmpty())throw new com.web.sms.exception.BadRequestException("Unassign this In-Charge from their bus before deactivation");i.setStatus(next);inchargeRepo.save(i);auditService.log(getCurrentUser().getUsername(),"ADMIN","INCHARGE_STATUS_UPDATED","INCHARGE",String.valueOf(id),null,next,null);return Map.of("id",id,"status",next);}

    @PostMapping("/boarding-points")
    public BoardingPointResponse createBoardingPoint(@Valid @RequestBody CreateBoardingPointRequest req) {
        Bus bus = busRepo.findById(req.getBusId())
                .orElseThrow(() -> new ResourceNotFoundException("Bus not found with id: " + req.getBusId()));
        if(bus.getStatus()!=EntityStatus.ACTIVE)throw new com.web.sms.exception.BadRequestException("Boarding points can only be added to an active bus");
        String stationName;
        if (req.getExistingPointId() != null) {
            BoardingPoints source = boardingPointsRepo.findById(req.getExistingPointId())
                    .orElseThrow(() -> new ResourceNotFoundException("Selected boarding point was not found"));
            stationName = source.getStationName();
        } else {
            if (req.getStationName() == null || req.getStationName().isBlank()) {
                throw new com.web.sms.exception.BadRequestException("Select an available boarding point or enter a new point name");
            }
            stationName = req.getStationName().trim();
            boardingPointsRepo.findFirstByStationNameIgnoreCase(stationName)
                    .ifPresent(existing -> { throw new com.web.sms.exception.BadRequestException(
                            "This boarding point name already exists. Select it from the available points list"); });
        }
        if(boardingPointsRepo.existsByBusIdAndStationNameIgnoreCase(bus.getId(),stationName))throw new com.web.sms.exception.BadRequestException("This boarding point already exists for the bus");

        BoardingPoints bp = new BoardingPoints();
        bp.setBus(bus);
        bp.setStationName(stationName);
        bp.setFeeAmount(req.getFeeAmount());
        bp.setOrderIndex(req.getOrderIndex() != null ? req.getOrderIndex() : 0);
        bp.setStatus(EntityStatus.ACTIVE);

        BoardingPoints saved = boardingPointsRepo.save(bp);
        auditService.log(getCurrentUser().getUsername(),"ADMIN","BOARDING_POINT_CREATED","BOARDING_POINT",String.valueOf(saved.getId()),null,saved.getStationName(),null);
        return BoardingPointResponse.fromBoardingPoint(saved);
    }

    @GetMapping("/buses/{busId}/boarding-points")
    public List<BoardingPointResponse> getBoardingPoints(@PathVariable Long busId) {
        if (!busRepo.existsById(busId)) throw new ResourceNotFoundException("Bus not found");
        return boardingPointsRepo.findByBusIdOrderByOrderIndexAscStationNameAsc(busId).stream().map(BoardingPointResponse::fromBoardingPoint).toList();
    }

    @PutMapping("/buses/{busId}/boarding-points/order")
    @Transactional
    public List<BoardingPointResponse> reorderBoardingPoints(@PathVariable Long busId,
            @Valid @RequestBody ReorderBoardingPointsRequest req) {
        if (!busRepo.existsById(busId)) throw new ResourceNotFoundException("Bus not found");
        List<BoardingPoints> points = boardingPointsRepo.findByBusIdForUpdate(busId);
        List<Long> requestedIds = req.getBoardingPointIds();
        java.util.Set<Long> requestedSet = new java.util.HashSet<>(requestedIds);
        java.util.Set<Long> currentSet = points.stream().map(BoardingPoints::getId).collect(java.util.stream.Collectors.toSet());
        if (requestedIds.size() != requestedSet.size() || !requestedSet.equals(currentSet)) {
            throw new com.web.sms.exception.BadRequestException(
                    "The order must contain every boarding point for this bus exactly once");
        }
        Map<Long, BoardingPoints> byId = points.stream().collect(java.util.stream.Collectors.toMap(BoardingPoints::getId, point -> point));
        java.util.ArrayList<BoardingPoints> ordered = new java.util.ArrayList<>();
        for (int index = 0; index < requestedIds.size(); index++) {
            BoardingPoints point = byId.get(requestedIds.get(index));
            point.setOrderIndex(index);
            ordered.add(point);
        }
        boardingPointsRepo.saveAll(ordered);
        auditService.log(getCurrentUser().getUsername(), "ADMIN", "BOARDING_POINTS_REORDERED",
                "BUS", String.valueOf(busId), null, requestedIds.toString(), null);
        return ordered.stream().map(BoardingPointResponse::fromBoardingPoint).toList();
    }

    @GetMapping("/boarding-points")
    public List<BoardingPointResponse> getAllBoardingPoints() {
        return boardingPointsRepo.findAllByOrderByBusBusNumberAscOrderIndexAscStationNameAsc()
                .stream().map(BoardingPointResponse::fromBoardingPoint).toList();
    }

    @PutMapping("/boarding-points/{id}")
    @Transactional
    public BoardingPointResponse updateBoardingPoint(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateBoardingPointRequest req) {
        BoardingPoints point = boardingPointsRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Boarding point not found"));
        String stationName = req.getStationName().trim();
        if (!point.getStationName().equalsIgnoreCase(stationName)
                && boardingPointsRepo.existsByBusIdAndStationNameIgnoreCase(point.getBus().getId(), stationName)) {
            throw new com.web.sms.exception.BadRequestException("This boarding point already exists for the bus");
        }
        String oldValue = point.getStationName() + " / ₹" + point.getFeeAmount();
        point.setStationName(stationName);
        point.setFeeAmount(req.getFeeAmount());
        point.setOrderIndex(req.getOrderIndex() == null ? 0 : req.getOrderIndex());
        BoardingPoints saved = boardingPointsRepo.save(point);
        auditService.log(getCurrentUser().getUsername(), "ADMIN", "BOARDING_POINT_UPDATED",
                "BOARDING_POINT", String.valueOf(id), oldValue,
                saved.getStationName() + " / ₹" + saved.getFeeAmount(), null);
        return BoardingPointResponse.fromBoardingPoint(saved);
    }

    @DeleteMapping("/boarding-points/{id}")
    @Transactional
    public void deleteBoardingPoint(@PathVariable Long id) {
        BoardingPoints point = boardingPointsRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Boarding point not found"));
        long allocations = allocationRepo.countByBoardingPointId(id);
        long applications = applicationRepo.countByBoardingPointId(id);
        long transfers = transferRepo.countByCurrentBoardingPointIdOrRequestedBoardingPointId(id, id);
        if (allocations + applications + transfers > 0) {
            throw new com.web.sms.exception.BadRequestException(
                    "Cannot delete a boarding point referenced by applications, allocations or transfers. Deactivate it instead.");
        }
        boardingPointsRepo.delete(point);
        auditService.log(getCurrentUser().getUsername(), "ADMIN", "BOARDING_POINT_DELETED",
                "BOARDING_POINT", String.valueOf(id), point.getStationName(), null, null);
    }

    @GetMapping("/incharges")
    public List<Incharge> getIncharges() {
        return inchargeService.getAllIncharges();
    }

    @PostMapping("/incharges")
    public Incharge createIncharge(@Valid @RequestBody CreateInchargeRequest req) {
        Incharge i=inchargeService.createIncharge(req);auditService.log(getCurrentUser().getUsername(),"ADMIN","INCHARGE_CREATED","INCHARGE",String.valueOf(i.getId()),null,i.getTeacherId(),null);return i;
    }

    @PutMapping("/incharges/{id}")
    public Incharge updateIncharge(@PathVariable Long id, @Valid @RequestBody CreateInchargeRequest req) {
        Incharge i=inchargeService.updateIncharge(id, req);auditService.log(getCurrentUser().getUsername(),"ADMIN","INCHARGE_UPDATED","INCHARGE",String.valueOf(id),null,i.getTeacherId(),null);return i;
    }
    @GetMapping("/incharges/{id}/photo") public ResponseEntity<byte[]> inchargePhoto(@PathVariable Long id){Incharge i=inchargeService.getInchargeById(id);if(i.getPhotoData()==null||i.getPhotoData().length==0)throw new ResourceNotFoundException("In-Charge photo not found");return ResponseEntity.ok().contentType(MediaType.parseMediaType(i.getPhotoContentType()==null?MediaType.APPLICATION_OCTET_STREAM_VALUE:i.getPhotoContentType())).body(i.getPhotoData());}
    @PutMapping(value="/incharges/{id}/photo",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @Transactional public Map<String,Boolean> uploadInchargePhoto(@PathVariable Long id,@RequestPart("photo") MultipartFile photo) throws java.io.IOException {if(photo==null||photo.isEmpty()||photo.getSize()>2L*1024*1024)throw new com.web.sms.exception.BadRequestException("Photo must be a JPEG or PNG image up to 2 MB");byte[] bytes=photo.getBytes();boolean jpeg=bytes.length>=3&&(bytes[0]&0xff)==0xff&&(bytes[1]&0xff)==0xd8&&(bytes[2]&0xff)==0xff;boolean png=bytes.length>=8&&(bytes[0]&0xff)==0x89&&bytes[1]==0x50&&bytes[2]==0x4e&&bytes[3]==0x47;if(!jpeg&&!png)throw new com.web.sms.exception.BadRequestException("Photo must be a valid JPEG or PNG image");Incharge i=inchargeService.getInchargeById(id);i.setPhotoData(bytes);i.setPhotoContentType(jpeg?MediaType.IMAGE_JPEG_VALUE:MediaType.IMAGE_PNG_VALUE);auditService.log(getCurrentUser().getUsername(),"ADMIN","INCHARGE_PHOTO_UPDATED","INCHARGE",String.valueOf(id),null,i.getTeacherId(),null);return Map.of("photoAvailable",true);}

    @GetMapping("/students")
    public Page<StudentProfileResponse> getStudents(@RequestParam(required = false) String search,
                                                   @RequestParam(required = false) String branch,
                                                   @RequestParam(defaultValue = "false") boolean unpaged, Pageable pageable) {
        return studentRepo.searchStudents(search, branch, requestedPage(unpaged, pageable))
                .map(StudentProfileResponse::fromStudent);
    }

    @PostMapping(value="/students",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public StudentProfileResponse createStudent(@Valid @RequestPart("student") CreateStudentRequest req,@RequestPart("photo") MultipartFile photo) {
        return adminService.createStudent(req,photo);
    }

    @PutMapping(value="/students/{id}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public StudentProfileResponse updateStudent(@PathVariable Long id,@Valid @RequestPart("student") UpdateStudentRequest req,@RequestPart(value="photo",required=false) MultipartFile photo){return adminService.updateStudent(id,req,photo,getCurrentUser().getUsername());}

    @DeleteMapping("/students/{id}") public void deleteStudent(@PathVariable Long id){adminService.deleteStudent(id,getCurrentUser().getUsername());}

    @PutMapping("/students/{id}/status")
    public StudentProfileResponse updateStudentStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest req) {
        return adminService.updateStudentStatus(id, req.getStatus(), getCurrentUser().getUsername());
    }

    @GetMapping("/applications")
    public Page<ApplicationResponse> getApplications(@RequestParam(required = false) String search, @RequestParam(required = false) ApplicationStatus status, @RequestParam(defaultValue = "false") boolean unpaged, Pageable pageable) {
        pageable=requestedPage(unpaged, pageable);
        return applicationRepo.searchForAdmin(search == null ? "" : search.trim(), status, pageable).map(ApplicationResponse::fromApplication);
    }

    @GetMapping("/applications/summary")
    public Map<String, Long> applicationSummary() {
        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("registered", studentRepo.count());
        for (ApplicationStatus status : ApplicationStatus.values()) summary.put(status.name().toLowerCase(), applicationRepo.countByStatus(status));
        return summary;
    }

    @PutMapping("/applications/{id}/approve")
    public ApplicationResponse approveApplication(@PathVariable Long id) {
        return applicationService.approveApplication(id, getCurrentUser().getUsername());
    }

    @PutMapping("/applications/{id}/reject")
    public ApplicationResponse rejectApplication(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        // Accept both {"remarks":"..."} JSON body and null/empty
        String remarks = (body != null) ? body.getOrDefault("remarks", "").trim() : "";
        String finalRemarks = remarks.isEmpty() ? "Rejected by Administrator" : remarks;
        return applicationService.rejectApplication(id, finalRemarks, getCurrentUser().getUsername());
    }
    @PutMapping("/applications/{id}") public ApplicationResponse updateApplication(@PathVariable Long id,@Valid @RequestBody UpdateApplicationRequest req){return applicationService.updateApplication(id,req,getCurrentUser().getUsername());}
    @DeleteMapping("/applications/{id}") public void deleteApplication(@PathVariable Long id){applicationService.deleteApplication(id,getCurrentUser().getUsername());}

    @GetMapping("/fees")
    public Page<FeeResponse> getFees(@RequestParam(required = false) String search, @RequestParam(defaultValue = "false") boolean unpaged, Pageable pageable) {
        String trimmed = (search == null || search.isBlank()) ? null : search.trim();
        if (trimmed == null) {
            return feeRepo.findAll(requestedPage(unpaged, pageable)).map(FeeResponse::fromFee);
        }
        return feeRepo.searchForAdmin(trimmed, requestedPage(unpaged, pageable)).map(FeeResponse::fromFee);
    }

    @PostMapping("/fees/{id}/manual-payment")
    public FeeResponse recordManualPayment(@PathVariable Long id, @Valid @RequestBody AdminManualPaymentRequest req) {
        FeeResponse result = feeService.recordManualPayment(id, req, getCurrentUser().getUsername());
        auditService.log(getCurrentUser().getUsername(), "ADMIN", "MANUAL_PAYMENT_RECORDED", "FEE",
                String.valueOf(id), null, String.valueOf(req.getAmount()), req.getRemarks());
        return result;
    }

    @GetMapping("/transfers")
    public Page<TransferResponse> getTransfers(@RequestParam(required = false) String status, @RequestParam(defaultValue = "false") boolean unpaged, Pageable pageable) {
        // Simple status filter; full text search can be added to TransferRequestRepository if needed
        if (status != null && !status.isBlank()) {
            try {
                com.web.sms.enums.TransferStatus ts = com.web.sms.enums.TransferStatus.valueOf(status.trim().toUpperCase());
                return transferRepo.findByStatus(ts, requestedPage(unpaged, pageable)).map(TransferResponse::fromTransfer);
            } catch (IllegalArgumentException e) {
                throw new com.web.sms.exception.BadRequestException("Invalid transfer status: " + status);
            }
        }
        return transferRepo.findAllWithDetails(requestedPage(unpaged, pageable)).map(TransferResponse::fromTransfer);
    }
    @GetMapping("/transfers/summary") public Map<String, Long> transferSummary(){Map<String,Long> summary=new LinkedHashMap<>();summary.put("total",transferRepo.count());summary.put("accepted",transferRepo.countByStatus(TransferStatus.TRANSFER_COMPLETED));summary.put("rejected",transferRepo.countByStatus(TransferStatus.OLD_INCHARGE_REJECTED)+transferRepo.countByStatus(TransferStatus.NEW_INCHARGE_REJECTED));return summary;}
    @PostMapping("/transfers/{id}/approve") public TransferResponse approveTransfer(@PathVariable Long id){return transferService.approveByAdmin(id,getCurrentUser().getUsername());}

    @GetMapping("/payments") public Page<PaymentResponse> payments(@RequestParam(defaultValue="false") boolean unpaged,Pageable pageable){return paymentRepo.findAll(requestedPage(unpaged,pageable)).map(PaymentResponse::fromPayment);}
    @GetMapping("/passes") public Page<PassResponse> passes(@RequestParam(required=false) String search,@RequestParam(defaultValue="false") boolean unpaged,Pageable pageable){return passRepo.search(search==null?null:search.trim(),requestedPage(unpaged,pageable)).map(PassResponse::fromBusPass);}
    @GetMapping("/passes/{id}") public PassResponse pass(@PathVariable Long id){return passService.getPassForAdmin(id);}
    @GetMapping("/passes/{id}/photo") public ResponseEntity<byte[]> passPhoto(@PathVariable Long id){return passService.passPhotoForAdmin(id);}
    @GetMapping("/passes/{id}/download") public ResponseEntity<byte[]> downloadPass(@PathVariable Long id){PassResponse pass=passService.getPassForAdmin(id);return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=bus-pass-"+pass.getRollNumber()+".pdf").body(passService.generatePdfForAdmin(id));}
    @GetMapping("/notifications") public Page<NotificationResponse> notifications(@RequestParam(defaultValue="false") boolean unpaged,Pageable pageable){return notificationRepo.findAll(requestedPage(unpaged,pageable)).map(NotificationResponse::fromNotification);}
    @GetMapping("/profile") public Map<String,Object> profile(){Admin a=adminRepo.findById(getCurrentUser().getId()).orElseThrow(()->new ResourceNotFoundException("Admin not found"));Map<String,Object> r=new LinkedHashMap<>();r.put("adminId",a.getAdminId());r.put("name",a.getName());r.put("email",a.getEmail());r.put("phoneNumber",a.getPhoneNumber());r.put("status",a.getStatus());return r;}
    @GetMapping("/students/{id}/transport") public Map<String,Object> studentTransport(@PathVariable Long id){Student s=studentRepo.findById(id).orElseThrow(()->new ResourceNotFoundException("Student not found"));Map<String,Object> r=new LinkedHashMap<>();r.put("student",StudentProfileResponse.fromStudent(s));java.util.List<TransportAllocation> history=allocationRepo.findByStudentId(id);r.put("assignments",history.stream().map(a->{Map<String,Object>x=new LinkedHashMap<>();x.put("id",a.getId());x.put("bus",a.getBus().getBusNumber());x.put("boardingPoint",a.getBoardingPoint().getStationName());x.put("status",a.getStatus());return x;}).toList());r.put("fees",feeRepo.findByStudentId(id).stream().map(FeeResponse::fromFee).toList());r.put("passes",passRepo.findByStudentId(id).stream().map(PassResponse::fromBusPass).toList());r.put("transfers",transferRepo.findByStudentId(id).stream().map(TransferResponse::fromTransfer).toList());return r;}

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

    @PostMapping("/academic-years")
    public AcademicYear createAcademicYear(@Valid @RequestBody CreateAcademicYearRequest req) {
        return adminService.createAcademicYear(req);
    }

    @GetMapping("/academic-years")
    public List<AcademicYear> getAcademicYears() {
        return academicYearRepo.findAll();
    }

    @GetMapping("/audit-logs")
    public Page<AuditLog> getAuditLogs(@RequestParam(defaultValue="false") boolean unpaged, Pageable pageable) {
        return auditService.getLogs(requestedPage(unpaged, pageable));
    }

    @GetMapping("/reports/summary")
    public DashboardResponse getSummaryReport() {
        return adminService.getDashboard();
    }
    @GetMapping("/reports/students") public Page<TransportReportRow> studentReport(@ModelAttribute TransportReportFilter filter,@RequestParam(defaultValue="false") boolean unpaged,Pageable pageable){return reportService.report(null,filter,requestedPage(unpaged,pageable));}
    @GetMapping("/reports/students/export/{format}") public ResponseEntity<byte[]> exportStudentReport(@PathVariable String format,@ModelAttribute TransportReportFilter filter){return reportDownload(format,filter,null,"LBRCE College Transport Report");}
    private ResponseEntity<byte[]> reportDownload(String format,TransportReportFilter filter,Long busId,String title){boolean excel="excel".equalsIgnoreCase(format)||"xlsx".equalsIgnoreCase(format);boolean pdf="pdf".equalsIgnoreCase(format);if(!excel&&!pdf)throw new com.web.sms.exception.BadRequestException("Export format must be excel or pdf");byte[] body=excel?reportService.excel(busId,filter,title):reportService.pdf(busId,filter,title);String ext=excel?"xlsx":"pdf";MediaType type=excel?MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"):MediaType.APPLICATION_PDF;return ResponseEntity.ok().contentType(type).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=transport-report."+ext).body(body);}
    private Pageable requestedPage(boolean unpaged, Pageable p){return unpaged ? Pageable.unpaged() : checked(p);}
    private Pageable checked(Pageable p){if(p.getPageNumber()<0||p.getPageSize()<1||p.getPageSize()>200)throw new com.web.sms.exception.BadRequestException("Page size must be between 1 and 200");return p;}
    private EntityStatus parseEntityStatus(String s){try{return EntityStatus.valueOf(s.toUpperCase());}catch(Exception e){throw new com.web.sms.exception.BadRequestException("Invalid status");}}
}
