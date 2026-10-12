package com.web.sms.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.web.sms.dto.request.AdminManualPaymentRequest;
import com.web.sms.dto.request.PaymentRequest;
import com.web.sms.dto.request.RazorpayVerificationRequest;
import com.web.sms.dto.request.AssignInchargeRequest;
import com.web.sms.dto.request.CreateAcademicYearRequest;
import com.web.sms.dto.request.CreateBoardingPointRequest;
import com.web.sms.dto.request.CreateBusRequest;
import com.web.sms.dto.request.CreateInchargeRequest;
import com.web.sms.dto.request.CreateStudentRequest;
import com.web.sms.dto.request.ReorderBoardingPointsRequest;
import com.web.sms.dto.request.StatusUpdateRequest;
import com.web.sms.dto.request.TransportReportFilter;
import com.web.sms.dto.request.UpdateApplicationRequest;
import com.web.sms.dto.request.UpdateBoardingPointRequest;
import com.web.sms.dto.request.UpdateStudentRequest;
import com.web.sms.dto.response.ApplicationResponse;
import com.web.sms.dto.response.BoardingPointResponse;
import com.web.sms.dto.response.BusResponse;
import com.web.sms.dto.response.DashboardResponse;
import com.web.sms.dto.response.FeeResponse;
import com.web.sms.dto.response.NotificationResponse;
import com.web.sms.dto.response.PassResponse;
import com.web.sms.dto.response.PaymentResponse;
import com.web.sms.dto.response.RazorpayOrderResponse;
import com.web.sms.dto.response.StudentProfileResponse;
import com.web.sms.dto.response.TransferResponse;
import com.web.sms.dto.response.TransportReportRow;
import com.web.sms.entity.AcademicYear;
import com.web.sms.entity.Admin;
import com.web.sms.entity.AuditLog;
import com.web.sms.entity.BoardingPoints;
import com.web.sms.entity.Bus;
import com.web.sms.entity.BusApplication;
import com.web.sms.entity.Incharge;
import com.web.sms.entity.Student;
import com.web.sms.entity.TransportAllocation;
import com.web.sms.enums.ApplicationStatus;
import com.web.sms.enums.EntityStatus;
import com.web.sms.enums.TransferStatus;
import com.web.sms.exception.ResourceNotFoundException;
import com.web.sms.repository.AcademicYearRepository;
import com.web.sms.repository.AdminRepository;
import com.web.sms.repository.BoardingPointsRepository;
import com.web.sms.repository.BusApplicationRepository;
import com.web.sms.repository.BusPassRepository;
import com.web.sms.repository.BusRepository;
import com.web.sms.repository.FeeRepository;
import com.web.sms.repository.InchargeRepository;
import com.web.sms.repository.NotificationRepository;
import com.web.sms.repository.PaymentRepository;
import com.web.sms.repository.StudentRepository;
import com.web.sms.repository.StudentAcademicDetailsRepository;
import com.web.sms.repository.TransferRequestRepository;
import com.web.sms.repository.TransportAllocationRepository;
import com.web.sms.security.UserPrincipal;
import com.web.sms.service.AdminService;
import com.web.sms.service.ApplicationService;
import com.web.sms.service.AuditService;
import com.web.sms.service.BusService;
import com.web.sms.service.FeeService;
import com.web.sms.service.InchargeService;
import com.web.sms.service.PassService;
import com.web.sms.service.TransferService;
import com.web.sms.service.TransportReportService;
import com.web.sms.service.StudentHistoryService;
import com.web.sms.service.RazorpayPaymentService;

import jakarta.validation.Valid;

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
    private final StudentAcademicDetailsRepository studentAcademicDetailsRepo;
    private final StudentHistoryService studentHistoryService;
    private final RazorpayPaymentService razorpayPaymentService;

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
                           PassService passService, FeeService feeService, StudentAcademicDetailsRepository studentAcademicDetailsRepo,
                           StudentHistoryService studentHistoryService,
                           RazorpayPaymentService razorpayPaymentService) {
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
        this.studentAcademicDetailsRepo=studentAcademicDetailsRepo;
        this.studentHistoryService=studentHistoryService;
        this.razorpayPaymentService=razorpayPaymentService;
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
        if (req.getOrderIndex() != null) {
            bp.setOrderIndex(req.getOrderIndex());
        }
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
        if (req.getOrderIndex() != null) {
            point.setOrderIndex(req.getOrderIndex());
        } else {
            point.setOrderIndex(0);
        }
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

    @GetMapping({"/students", "/students/all"})
    public List<StudentProfileResponse> getStudents(@RequestParam(required = false) String search,
                                                    @RequestParam(required = false) String branch,
                                                    @RequestParam(required = false) Long academicYearId) {
        String term = search == null ? "" : search.trim().toLowerCase();
        String branchFilter = (branch == null || branch.isBlank()) ? null : branch.trim();
        Long yearId = academicYearId != null ? academicYearId : academicYearRepo.findByActiveTrue().map(AcademicYear::getId).orElse(null);
        return studentRepo.findAll().stream()
                .map(s -> yearId == null ? StudentProfileResponse.fromStudent(s) : studentAcademicDetailsRepo.findByStudentIdAndAcademicYearId(s.getId(), yearId)
                        .map(details -> StudentProfileResponse.fromStudent(s, details)).orElse(null))
                .filter(java.util.Objects::nonNull)
                .filter(s -> branchFilter == null || branchFilter.equalsIgnoreCase(s.getBranch()))
                .filter(s -> term.isEmpty() || String.join(" ",
                        safe(s.getName()), safe(s.getRollNumber()), safe(s.getEmail()),
                        safe(s.getPhoneNumber()), safe(s.getBranch()),
                        s.getYear() == null ? "" : String.valueOf(s.getYear()),
                        s.getSemester() == null ? "" : String.valueOf(s.getSemester()),
                        safe(s.getStatus()))
                        .toLowerCase().contains(term))
                .toList();
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

    @GetMapping("/students/{id}/photo")
    public ResponseEntity<byte[]> getStudentPhoto(@PathVariable Long id) {
        Student st = studentRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        if (st.getPhotoData() == null || st.getPhotoData().length == 0) throw new ResourceNotFoundException("Student photo not found");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(st.getPhotoContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : st.getPhotoContentType()))
                .body(st.getPhotoData());
    }

    @GetMapping("/students/{id}/academic-history")
    public List<Map<String, Object>> studentAcademicHistory(@PathVariable Long id) {
        if (!studentRepo.existsById(id)) throw new ResourceNotFoundException("Student not found");
        return studentAcademicDetailsRepo.findByStudentIdOrderByAcademicYearStartDateDesc(id).stream().map(d -> Map.<String, Object>of(
                "id", d.getId(), "academicYearId", d.getAcademicYear().getId(), "academicYear", d.getAcademicYear().getYearName(),
                "branch", d.getBranch(), "year", d.getYear(), "semester", d.getSemester(), "status", d.getStatus())).toList();
    }

    @PutMapping("/academic-years/{academicYearId}/students/{studentId}")
    public StudentProfileResponse setStudentAcademicDetails(@PathVariable Long academicYearId, @PathVariable Long studentId,
                                                             @Valid @RequestBody com.web.sms.dto.request.StudentAcademicDetailsRequest request) {
        return adminService.setStudentAcademicDetails(studentId, academicYearId, request, getCurrentUser().getUsername());
    }

    @GetMapping({"/applications", "/applications/all"})
    public List<ApplicationResponse> getApplications(@RequestParam(required = false) String search,
                                                     @RequestParam(required = false) ApplicationStatus status,
                                                     @RequestParam(required = false) Long academicYearId) {
        String term = search == null ? "" : search.trim();
        return applicationRepo.searchAdminList(term,status,academicYearId).stream()
                .map(ApplicationResponse::fromApplication)
                .toList();
    }

    private String safe(String value) { return value == null ? "" : value; }

    @GetMapping("/applications/summary")
    public Map<String, Long> applicationSummary(@RequestParam(required = false) Long academicYearId) {
        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("registered", applicationRepo.countForSummary(academicYearId,null));
        for (ApplicationStatus status : ApplicationStatus.values()) summary.put(status.name().toLowerCase(), applicationRepo.countForSummary(academicYearId,status));
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

    @GetMapping("/applications/{id}/student-details")
    public StudentProfileResponse getApplicationStudentDetails(@PathVariable Long id) {
        BusApplication app = applicationRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        return studentAcademicDetailsRepo.findByStudentIdAndAcademicYearId(app.getStudent().getId(),app.getAcademicYear().getId())
                .map(details -> StudentProfileResponse.fromStudent(app.getStudent(),details))
                .orElseGet(() -> StudentProfileResponse.fromStudent(app.getStudent()));
    }

    @GetMapping("/applications/{id}/student-photo")
    public ResponseEntity<byte[]> getApplicationStudentPhoto(@PathVariable Long id) {
        BusApplication app = applicationRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        Student st = app.getStudent();
        if (st.getPhotoData() == null || st.getPhotoData().length == 0) throw new ResourceNotFoundException("Student photo not found");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(st.getPhotoContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : st.getPhotoContentType())).body(st.getPhotoData());
    }

    @GetMapping({"/fees", "/fees/all"})
    public List<FeeResponse> getFees(@RequestParam(required = false) String search, @RequestParam(required = false) Long academicYearId) {
        String trimmed = (search == null || search.isBlank()) ? null : search.trim();
        if (trimmed == null) {
            return feeRepo.findAll().stream().filter(f -> academicYearId == null || f.getAcademicYear().getId().equals(academicYearId)).map(FeeResponse::fromFee).toList();
        }
        return feeRepo.searchForAdminList(trimmed).stream().filter(f -> academicYearId == null || f.getAcademicYear().getId().equals(academicYearId)).map(FeeResponse::fromFee).toList();
    }

    @PostMapping("/fees/{id}/manual-payment")
    public FeeResponse recordManualPayment(@PathVariable Long id, @Valid @RequestBody AdminManualPaymentRequest req) {
        FeeResponse result = feeService.recordManualPayment(id, req, getCurrentUser().getUsername());
        auditService.log(getCurrentUser().getUsername(), "ADMIN", "MANUAL_PAYMENT_RECORDED", "FEE",
                String.valueOf(id), null, String.valueOf(req.getAmount()), req.getRemarks());
        return result;
    }

    @PostMapping("/payments/order")
    public RazorpayOrderResponse createPaymentOrder(@Valid @RequestBody PaymentRequest req) {
        return razorpayPaymentService.createStaffOrder(getCurrentUser().getId(), getCurrentUser().getRole(), req);
    }

    @PostMapping("/payments/verify")
    public PaymentResponse verifyPayment(@Valid @RequestBody RazorpayVerificationRequest req) {
        return razorpayPaymentService.verifyStaffCheckout(getCurrentUser().getId(), getCurrentUser().getRole(), req);
    }

    @GetMapping({"/transfers", "/transfers/all"})
    public List<TransferResponse> getTransfers(@RequestParam(required = false) String status, @RequestParam(required = false) Long academicYearId) {
        // Simple status filter; full text search can be added to TransferRequestRepository if needed
        if (status != null && !status.isBlank()) {
            try {
                com.web.sms.enums.TransferStatus ts = com.web.sms.enums.TransferStatus.valueOf(status.trim().toUpperCase());
                return transferRepo.findByStatusList(ts).stream().filter(t -> academicYearId == null || t.getAcademicYear().getId().equals(academicYearId)).map(TransferResponse::fromTransfer).toList();
            } catch (IllegalArgumentException e) {
                throw new com.web.sms.exception.BadRequestException("Invalid transfer status: " + status);
            }
        }
        return transferRepo.findAllWithDetailsList().stream().filter(t -> academicYearId == null || t.getAcademicYear().getId().equals(academicYearId)).map(TransferResponse::fromTransfer).toList();
    }
    @GetMapping("/transfers/summary") public Map<String, Long> transferSummary(){Map<String,Long> summary=new LinkedHashMap<>();summary.put("total",transferRepo.count());summary.put("accepted",transferRepo.countByStatus(TransferStatus.TRANSFER_COMPLETED));summary.put("rejected",transferRepo.countByStatus(TransferStatus.OLD_INCHARGE_REJECTED)+transferRepo.countByStatus(TransferStatus.NEW_INCHARGE_REJECTED));return summary;}
    @PostMapping("/transfers/{id}/approve") public TransferResponse approveTransfer(@PathVariable Long id){return transferService.approveByAdmin(id,getCurrentUser().getUsername());}

    @GetMapping("/payments") public List<PaymentResponse> payments(@RequestParam(required = false) Long academicYearId){return paymentRepo.findAll().stream().filter(p -> academicYearId == null || (p.getFee() != null && p.getFee().getAcademicYear() != null && p.getFee().getAcademicYear().getId().equals(academicYearId))).map(PaymentResponse::fromPayment).toList();}
    @GetMapping("/passes") public List<PassResponse> passes(@RequestParam(required=false) String search,@RequestParam(required=false) Long academicYearId){return passRepo.searchList(search==null?null:search.trim()).stream().filter(p -> academicYearId == null || p.getAcademicYear().getId().equals(academicYearId)).map(PassResponse::fromBusPass).toList();}
    @GetMapping("/passes/{id}") public PassResponse pass(@PathVariable Long id){return passService.getPassForAdmin(id);}
    @GetMapping("/passes/{id}/photo") public ResponseEntity<byte[]> passPhoto(@PathVariable Long id){return passService.passPhotoForAdmin(id);}
    @GetMapping("/passes/{id}/download") public ResponseEntity<byte[]> downloadPass(@PathVariable Long id){PassResponse pass=passService.getPassForAdmin(id);return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=bus-pass-"+pass.getRollNumber()+".pdf").body(passService.generatePdfForAdmin(id));}
    @GetMapping("/notifications") public List<NotificationResponse> notifications(){return notificationRepo.findAll().stream().map(NotificationResponse::fromNotification).toList();}
    @GetMapping("/profile") public Map<String,Object> profile(){Admin a=adminRepo.findById(getCurrentUser().getId()).orElseThrow(()->new ResourceNotFoundException("Admin not found"));Map<String,Object> r=new LinkedHashMap<>();r.put("adminId",a.getAdminId());r.put("name",a.getName());r.put("email",a.getEmail());r.put("phoneNumber",a.getPhoneNumber());r.put("status",a.getStatus());return r;}
    @GetMapping("/students/{id}/transport") public Map<String,Object> studentTransport(@PathVariable Long id){Student s=studentRepo.findById(id).orElseThrow(()->new ResourceNotFoundException("Student not found"));Map<String,Object> r=new LinkedHashMap<>();r.put("student",StudentProfileResponse.fromStudent(s));var yearHistory=studentHistoryService.history(id);r.put("history",yearHistory);r.put("assignments",yearHistory.stream().flatMap(y->y.getAllocations().stream()).toList());r.put("fees",yearHistory.stream().flatMap(y->y.getFees().stream()).toList());r.put("passes",yearHistory.stream().flatMap(y->y.getPasses().stream()).toList());r.put("transfers",yearHistory.stream().flatMap(y->y.getTransfers().stream()).toList());r.put("applications",yearHistory.stream().flatMap(y->y.getApplications().stream()).toList());r.put("payments",yearHistory.stream().flatMap(y->y.getPayments().stream()).toList());return r;}


    @PostMapping("/academic-years")
    public AcademicYear createAcademicYear(@Valid @RequestBody CreateAcademicYearRequest req) {
        return adminService.createAcademicYear(req);
    }

    @GetMapping("/academic-years")
    public List<AcademicYear> getAcademicYears() {
        return academicYearRepo.findAll();
    }

    @GetMapping("/audit-logs")
    public List<AuditLog> getAuditLogs() {
        return auditService.getLogsList();
    }

    @GetMapping("/reports/summary")
    public DashboardResponse getSummaryReport() {
        return adminService.getDashboard();
    }
    @GetMapping("/reports/students") public List<TransportReportRow> studentReport(@ModelAttribute TransportReportFilter filter){return reportService.report(null,filter,Pageable.unpaged()).getContent();}
    @GetMapping("/reports/students/export/{format}") public ResponseEntity<byte[]> exportStudentReport(@PathVariable String format,@ModelAttribute TransportReportFilter filter){return reportDownload(format,filter,null,"LBRCE College Transport Report");}
    private ResponseEntity<byte[]> reportDownload(String format,TransportReportFilter filter,Long busId,String title){boolean excel="excel".equalsIgnoreCase(format)||"xlsx".equalsIgnoreCase(format);boolean pdf="pdf".equalsIgnoreCase(format);if(!excel&&!pdf)throw new com.web.sms.exception.BadRequestException("Export format must be excel or pdf");byte[] body=excel?reportService.excel(busId,filter,title):reportService.pdf(busId,filter,title);String ext=excel?"xlsx":"pdf";MediaType type=excel?MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"):MediaType.APPLICATION_PDF;return ResponseEntity.ok().contentType(type).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=transport-report."+ext).body(body);}
    private EntityStatus parseEntityStatus(String s){try{return EntityStatus.valueOf(s.toUpperCase());}catch(Exception e){throw new com.web.sms.exception.BadRequestException("Invalid status");}}
}
