package com.web.sms.controller;

import com.web.sms.dto.request.*;
import com.web.sms.dto.response.*;
import com.web.sms.entity.*;
import com.web.sms.enums.ApplicationStatus;
import com.web.sms.enums.EntityStatus;
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
                           PassService passService) {
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

    @GetMapping("/students")
    public Page<StudentProfileResponse> getStudents(@RequestParam(required = false) String search,
                                                   @RequestParam(required = false) String branch,
                                                   Pageable pageable) {
        return studentRepo.searchStudents(search, branch, checked(pageable))
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
    public Page<ApplicationResponse> getApplications(@RequestParam(required = false) ApplicationStatus status, Pageable pageable) {
        pageable=checked(pageable);
        if (status != null) {
            return applicationRepo.findByStatus(status, pageable).map(ApplicationResponse::fromApplication);
        }
        return applicationRepo.findAll(pageable).map(ApplicationResponse::fromApplication);
    }

    @PutMapping("/applications/{id}/approve")
    public ApplicationResponse approveApplication(@PathVariable Long id) {
        return applicationService.approveApplication(id, getCurrentUser().getUsername());
    }

    @PutMapping("/applications/{id}/reject")
    public ApplicationResponse rejectApplication(@PathVariable Long id, @RequestBody(required = false) String remarks) {
        String cleanRemarks = parseRemarks(remarks);
        return applicationService.rejectApplication(id, cleanRemarks != null ? cleanRemarks : "Rejected by Administrator", getCurrentUser().getUsername());
    }
    @PutMapping("/applications/{id}") public ApplicationResponse updateApplication(@PathVariable Long id,@Valid @RequestBody UpdateApplicationRequest req){return applicationService.updateApplication(id,req,getCurrentUser().getUsername());}
    @DeleteMapping("/applications/{id}") public void deleteApplication(@PathVariable Long id){applicationService.deleteApplication(id,getCurrentUser().getUsername());}

    @GetMapping("/fees")
    public Page<FeeResponse> getFees(Pageable pageable) {
        return feeRepo.findAll(checked(pageable)).map(FeeResponse::fromFee);
    }

    @GetMapping("/transfers")
    public Page<TransferResponse> getTransfers(Pageable pageable) {
        return transferRepo.findAllWithDetails(checked(pageable)).map(TransferResponse::fromTransfer);
    }
    @PostMapping("/transfers/{id}/approve") public TransferResponse approveTransfer(@PathVariable Long id){return transferService.approveByAdmin(id,getCurrentUser().getUsername());}

    @GetMapping("/payments") public Page<PaymentResponse> payments(Pageable pageable){return paymentRepo.findAll(checked(pageable)).map(PaymentResponse::fromPayment);}
    @GetMapping("/passes") public Page<PassResponse> passes(@RequestParam(required=false) String search,Pageable pageable){return passRepo.search(search==null?null:search.trim(),checked(pageable)).map(PassResponse::fromBusPass);}
    @GetMapping("/passes/{id}") public PassResponse pass(@PathVariable Long id){return passService.getPassForAdmin(id);}
    @GetMapping("/passes/{id}/photo") public ResponseEntity<byte[]> passPhoto(@PathVariable Long id){return passService.passPhotoForAdmin(id);}
    @GetMapping("/passes/{id}/download") public ResponseEntity<byte[]> downloadPass(@PathVariable Long id){PassResponse pass=passService.getPassForAdmin(id);return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=bus-pass-"+pass.getRollNumber()+".pdf").body(passService.generatePdfForAdmin(id));}
    @GetMapping("/notifications") public Page<NotificationResponse> notifications(Pageable pageable){return notificationRepo.findAll(checked(pageable)).map(NotificationResponse::fromNotification);}
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
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditService.getLogs(checked(pageable));
    }

    @GetMapping("/reports/summary")
    public DashboardResponse getSummaryReport() {
        return adminService.getDashboard();
    }
    @GetMapping("/reports/students") public Page<TransportReportRow> studentReport(@ModelAttribute TransportReportFilter filter,Pageable pageable){return reportService.report(null,filter,checked(pageable));}
    @GetMapping("/reports/students/export/{format}") public ResponseEntity<byte[]> exportStudentReport(@PathVariable String format,@ModelAttribute TransportReportFilter filter){return reportDownload(format,filter,null,"LBRCE College Transport Report");}
    private ResponseEntity<byte[]> reportDownload(String format,TransportReportFilter filter,Long busId,String title){boolean excel="excel".equalsIgnoreCase(format)||"xlsx".equalsIgnoreCase(format);boolean pdf="pdf".equalsIgnoreCase(format);if(!excel&&!pdf)throw new com.web.sms.exception.BadRequestException("Export format must be excel or pdf");byte[] body=excel?reportService.excel(busId,filter,title):reportService.pdf(busId,filter,title);String ext=excel?"xlsx":"pdf";MediaType type=excel?MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"):MediaType.APPLICATION_PDF;return ResponseEntity.ok().contentType(type).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=transport-report."+ext).body(body);}
    private Pageable checked(Pageable p){if(p.getPageNumber()<0||p.getPageSize()<1||p.getPageSize()>100)throw new com.web.sms.exception.BadRequestException("Page size must be between 1 and 100");return p;}
    private EntityStatus parseEntityStatus(String s){try{return EntityStatus.valueOf(s.toUpperCase());}catch(Exception e){throw new com.web.sms.exception.BadRequestException("Invalid status");}}
}
