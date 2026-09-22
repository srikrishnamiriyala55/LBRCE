package com.web.sms.service;

import com.web.sms.dto.request.CreateAcademicYearRequest;
import com.web.sms.dto.request.CreateStudentRequest;
import com.web.sms.dto.response.DashboardResponse;
import com.web.sms.dto.response.StudentProfileResponse;
import com.web.sms.entity.AcademicYear;
import com.web.sms.entity.Bus;
import com.web.sms.entity.Student;
import com.web.sms.enums.ApplicationStatus;
import com.web.sms.enums.EntityStatus;
import com.web.sms.enums.PassStatus;
import com.web.sms.enums.TransferStatus;
import com.web.sms.enums.ComplaintStatus;
import com.web.sms.exception.BadRequestException;
import com.web.sms.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminService {

    private final StudentRepository studentRepo;
    private final BusRepository busRepo;
    private final InchargeRepository inchargeRepo;
    private final BusApplicationRepository applicationRepo;
    private final BusPassRepository passRepo;
    private final FeeRepository feeRepo;
    private final TransportAllocationRepository allocationRepo;
    private final AcademicYearRepository academicYearRepo;
    private final PasswordEncoder passwordEncoder;
    private final TransferRequestRepository transferRepo;
    private final ComplaintRepository complaintRepo;
    private final AuditService auditService;

    public AdminService(StudentRepository studentRepo,
                        BusRepository busRepo,
                        InchargeRepository inchargeRepo,
                        BusApplicationRepository applicationRepo,
                        BusPassRepository passRepo,
                        FeeRepository feeRepo,
                        TransportAllocationRepository allocationRepo,
                        AcademicYearRepository academicYearRepo,
                        PasswordEncoder passwordEncoder, TransferRequestRepository transferRepo,
                        ComplaintRepository complaintRepo, AuditService auditService) {
        this.studentRepo = studentRepo;
        this.busRepo = busRepo;
        this.inchargeRepo = inchargeRepo;
        this.applicationRepo = applicationRepo;
        this.passRepo = passRepo;
        this.feeRepo = feeRepo;
        this.allocationRepo = allocationRepo;
        this.academicYearRepo = academicYearRepo;
        this.passwordEncoder = passwordEncoder;
        this.transferRepo = transferRepo;
        this.complaintRepo = complaintRepo;
        this.auditService = auditService;
    }

    @Transactional
    public com.web.sms.dto.response.BusResponse assignIncharge(Long busId,Long inchargeId,String actor){
        Bus bus=busRepo.findByIdForUpdate(busId).orElseThrow(()->new com.web.sms.exception.ResourceNotFoundException("Bus not found"));
        com.web.sms.entity.Incharge incharge=inchargeRepo.findByIdForUpdate(inchargeId).orElseThrow(()->new com.web.sms.exception.ResourceNotFoundException("In-Charge not found"));
        if(bus.getStatus()!=EntityStatus.ACTIVE)throw new BadRequestException("Only an active bus can receive an In-Charge");
        if(!"ACTIVE".equalsIgnoreCase(incharge.getStatus()))throw new BadRequestException("Only an active In-Charge can be assigned");
        java.util.List<Bus> existing=busRepo.findByInchargeId(inchargeId);
        if(!existing.isEmpty()&&!existing.get(0).getId().equals(busId))throw new BadRequestException("This In-Charge is already assigned to "+existing.get(0).getBusNumber());
        ensureNoPendingBusWorkflow(busId);
        String old=bus.getIncharge()==null?null:bus.getIncharge().getTeacherId();bus.setIncharge(incharge);bus.setUpdatedAt(LocalDateTime.now());busRepo.saveAndFlush(bus);
        auditService.log(actor,"ADMIN","BUS_INCHARGE_ASSIGNED","BUS",String.valueOf(busId),old,incharge.getTeacherId(),null);
        return com.web.sms.dto.response.BusResponse.fromBus(bus,(int)allocationRepo.countByBusIdAndStatus(busId,EntityStatus.ACTIVE));
    }

    @Transactional
    public com.web.sms.dto.response.BusResponse unassignIncharge(Long busId,String actor){
        Bus bus=busRepo.findByIdForUpdate(busId).orElseThrow(()->new com.web.sms.exception.ResourceNotFoundException("Bus not found"));
        if(bus.getIncharge()==null)return com.web.sms.dto.response.BusResponse.fromBus(bus,(int)allocationRepo.countByBusIdAndStatus(busId,EntityStatus.ACTIVE));
        ensureNoPendingBusWorkflow(busId);
        String old=bus.getIncharge().getTeacherId();bus.setIncharge(null);bus.setUpdatedAt(LocalDateTime.now());busRepo.save(bus);
        auditService.log(actor,"ADMIN","BUS_INCHARGE_UNASSIGNED","BUS",String.valueOf(busId),old,null,null);
        return com.web.sms.dto.response.BusResponse.fromBus(bus,(int)allocationRepo.countByBusIdAndStatus(busId,EntityStatus.ACTIVE));
    }

    private void ensureNoPendingBusWorkflow(Long busId){
        long applications=applicationRepo.countByBusIdAndStatusIn(busId,List.of(ApplicationStatus.PENDING,ApplicationStatus.UNDER_REVIEW));
        List<TransferStatus> active=List.of(TransferStatus.TRANSFER_REQUESTED,TransferStatus.OLD_INCHARGE_APPROVED,TransferStatus.NEW_INCHARGE_APPROVED);
        long transfers=transferRepo.countByCurrentBusIdAndStatusIn(busId,active)+transferRepo.countByRequestedBusIdAndStatusIn(busId,active);
        if(applications>0||transfers>0)throw new BadRequestException("Resolve "+applications+" pending application(s) and "+transfers+" active transfer workflow(s) before changing the bus In-Charge");
    }

    @Transactional
    public void deactivateBus(Long busId,String actor){Bus b=busRepo.findById(busId).orElseThrow(()->new com.web.sms.exception.ResourceNotFoundException("Bus not found"));long active=allocationRepo.countByBusIdAndStatus(busId,EntityStatus.ACTIVE);if(active>0)throw new BadRequestException("Cannot deactivate a bus with "+active+" active student assignments");b.setStatus(EntityStatus.INACTIVE);b.setUpdatedAt(LocalDateTime.now());busRepo.save(b);auditService.log(actor,"ADMIN","BUS_DEACTIVATED","BUS",String.valueOf(busId),"ACTIVE","INACTIVE",null);}

    public DashboardResponse getDashboard() {
        DashboardResponse res = new DashboardResponse();
        
        long totalStudents = studentRepo.count();
        long totalBuses = busRepo.count();
        long activeBuses = busRepo.countByStatus(EntityStatus.ACTIVE);
        long totalIncharges = inchargeRepo.count();
        
        long pendingApplications = applicationRepo.countByStatus(ApplicationStatus.PENDING);
        long totalCapacity = busRepo.sumActiveCapacity();
        long occupiedSeats = allocationRepo.countByStatus(EntityStatus.ACTIVE);

        long vacancy = Math.max(0, totalCapacity - occupiedSeats);

        long passesGenerated = passRepo.countByStatus(PassStatus.ACTIVE);

        Long totalFeeCollection = feeRepo.sumTotalPaidAmount();

        res.put("totalStudents", totalStudents);
        res.put("totalBuses", totalBuses);
        res.put("activeBuses", activeBuses);
        res.put("totalIncharges", totalIncharges);
        res.put("pendingApplications", pendingApplications);
        res.put("totalCapacity", totalCapacity);
        res.put("occupiedSeats", occupiedSeats);
        res.put("vacancy", vacancy);
        res.put("passesGenerated", passesGenerated);
        res.put("totalFeeCollection", totalFeeCollection != null ? totalFeeCollection : 0L);
        res.put("studentsWithTransportation", occupiedSeats);
        res.put("studentsWithoutTransportation", Math.max(0,totalStudents-occupiedSeats));
        res.put("approvedApplications", applicationRepo.countByStatus(ApplicationStatus.APPROVED));
        res.put("rejectedApplications", applicationRepo.countByStatus(ApplicationStatus.REJECTED));
        res.put("inactiveBuses", totalBuses-activeBuses);
        res.put("fullBuses", busRepo.countFullBuses());
        res.put("activeIncharges", inchargeRepo.countByStatusIgnoreCase("ACTIVE"));
        res.put("inactiveIncharges", inchargeRepo.countByStatusIgnoreCase("INACTIVE"));
        res.put("transferRequested", transferRepo.countByStatus(TransferStatus.TRANSFER_REQUESTED));
        res.put("waitingNewIncharge", transferRepo.countByStatus(TransferStatus.OLD_INCHARGE_APPROVED));
        res.put("completedTransfers", transferRepo.countByStatus(TransferStatus.TRANSFER_COMPLETED));
        res.put("rejectedTransfers", transferRepo.countByStatus(TransferStatus.OLD_INCHARGE_REJECTED)+transferRepo.countByStatus(TransferStatus.NEW_INCHARGE_REJECTED));
        long expected=feeRepo.sumTotalExpected(); res.put("totalExpectedFees",expected);res.put("pendingFeeAmount",Math.max(0,expected-(totalFeeCollection==null?0:totalFeeCollection)));res.put("passEligibleStudents",feeRepo.countByPassEligibleTrue());
        res.put("expiredOrRevokedPasses",passRepo.countByStatus(PassStatus.EXPIRED)+passRepo.countByStatus(PassStatus.REVOKED)+passRepo.countByStatus(PassStatus.INACTIVE));
        res.put("openComplaints",complaintRepo.countByStatus(ComplaintStatus.OPEN));res.put("inProgressComplaints",complaintRepo.countByStatus(ComplaintStatus.IN_PROGRESS));res.put("resolvedComplaints",complaintRepo.countByStatus(ComplaintStatus.RESOLVED));

        return res;
    }

    @Transactional
    public StudentProfileResponse createStudent(CreateStudentRequest req) {
        if (studentRepo.existsByRollNumber(req.getRollNumber())) {
            throw new BadRequestException("Student with roll number " + req.getRollNumber() + " already exists");
        }
        if (req.getEmail() != null && !req.getEmail().isBlank() && studentRepo.existsByEmail(req.getEmail())) {
            throw new BadRequestException("Student with email " + req.getEmail() + " already exists");
        }

        Student s = new Student();
        s.setRollNumber(req.getRollNumber().trim().toUpperCase());
        s.setName(req.getName().trim());
        s.setEmail(req.getEmail() != null ? req.getEmail().trim() : null);
        s.setPhoneNumber(req.getPhoneNumber());
        s.setPassword(passwordEncoder.encode(req.getPassword()));
        s.setBranch(req.getBranch());
        s.setYear(req.getYear());
        s.setSemester(req.getSemester());
        s.setGender(req.getGender());
        s.setAddress(req.getAddress());
        if (req.getDob() != null && !req.getDob().isBlank()) {
            s.setDob(LocalDate.parse(req.getDob()));
        }
        s.setBloodGroup(req.getBloodGroup());
        s.setParentName(req.getParentName());
        s.setParentPhoneNumber(req.getParentPhoneNumber());
        s.setStatus("ACTIVE");
        s.setCreatedAt(LocalDateTime.now());

        Student saved = studentRepo.save(s);
        return StudentProfileResponse.fromStudent(saved);
    }

    @Transactional
    public StudentProfileResponse updateStudentStatus(Long studentId, String requestedStatus, String actor) {
        Student student = studentRepo.findByIdForUpdate(studentId)
                .orElseThrow(() -> new com.web.sms.exception.ResourceNotFoundException("Student not found"));
        String next = requestedStatus == null ? "" : requestedStatus.trim().toUpperCase();
        if (!java.util.Set.of("ACTIVE", "INACTIVE").contains(next)) {
            throw new BadRequestException("Student status must be ACTIVE or INACTIVE");
        }
        String previous = student.getStatus();
        if (next.equalsIgnoreCase(previous)) return StudentProfileResponse.fromStudent(student);

        if ("INACTIVE".equals(next)) {
            LocalDateTime now = LocalDateTime.now();
            List<Bus> affectedBuses = new java.util.ArrayList<>();
            List<com.web.sms.entity.TransportAllocation> allocations = allocationRepo.findByStudentId(studentId);
            allocations.stream().filter(a -> a.getStatus() == EntityStatus.ACTIVE).forEach(a -> {
                a.setStatus(EntityStatus.INACTIVE);
                a.setDeactivatedAt(now);
                a.setSeatNumber(null);
                if (affectedBuses.stream().noneMatch(bus -> bus.getId().equals(a.getBus().getId()))) affectedBuses.add(a.getBus());
            });
            allocationRepo.saveAll(allocations);

            List<com.web.sms.entity.BusPass> passes = passRepo.findByStudentId(studentId);
            passes.stream().filter(p -> p.getStatus() == PassStatus.ACTIVE).forEach(p -> {
                p.setStatus(PassStatus.REVOKED);
                p.setRevokedAt(now);
            });
            passRepo.saveAll(passes);

            List<com.web.sms.entity.BusApplication> applications = applicationRepo.findByStudentId(studentId);
            applications.stream().filter(a -> a.getStatus() == ApplicationStatus.PENDING || a.getStatus() == ApplicationStatus.UNDER_REVIEW)
                    .forEach(a -> { a.setStatus(ApplicationStatus.CANCELLED); a.setReviewedAt(now); a.setReviewedBy(actor); a.setRemarks("Student account deactivated"); });
            applicationRepo.saveAll(applications);

            List<com.web.sms.entity.TransferRequest> transfers = transferRepo.findByStudentId(studentId);
            transfers.stream().filter(t -> java.util.Set.of(TransferStatus.TRANSFER_REQUESTED,
                            TransferStatus.OLD_INCHARGE_APPROVED, TransferStatus.NEW_INCHARGE_APPROVED).contains(t.getStatus()))
                    .forEach(t -> { t.setStatus(TransferStatus.CANCELLED); t.setProcessedAt(now); t.setRemarks("Student account deactivated"); });
            transferRepo.saveAll(transfers);

            allocationRepo.flush();
            affectedBuses.forEach(bus -> {
                long occupied = allocationRepo.countByBusIdAndStatus(bus.getId(), EntityStatus.ACTIVE);
                bus.setAvailableSeats(Math.max(0, bus.getTotalSeats() - (int) occupied));
                busRepo.save(bus);
            });
        }

        student.setStatus(next);
        Student saved = studentRepo.save(student);
        auditService.log(actor, "ADMIN", "STUDENT_STATUS_UPDATED", "STUDENT",
                String.valueOf(studentId), previous, next,
                "INACTIVE".equals(next) ? "Active allocations, passes, applications and transfers were safely closed" : null);
        return StudentProfileResponse.fromStudent(saved);
    }

    @Transactional
    public AcademicYear createAcademicYear(CreateAcademicYearRequest req) {
        if (academicYearRepo.findByYearName(req.getYearName()).isPresent()) {
            throw new BadRequestException("Academic year " + req.getYearName() + " already exists");
        }

        LocalDate start=req.getStartDate()!=null&&!req.getStartDate().isBlank()?LocalDate.parse(req.getStartDate()):null;
        LocalDate end=req.getEndDate()!=null&&!req.getEndDate().isBlank()?LocalDate.parse(req.getEndDate()):null;
        if(start!=null&&end!=null&&!end.isAfter(start))throw new BadRequestException("Academic year end date must be after its start date");
        // A single active academic year is maintained transactionally.
        academicYearRepo.findAll().forEach(y -> {
            y.setActive(false);
            academicYearRepo.save(y);
        });

        AcademicYear y = new AcademicYear();
        y.setYearName(req.getYearName());
        if (req.getStartDate() != null && !req.getStartDate().isBlank()) {
            y.setStartDate(start);
        }
        if (req.getEndDate() != null && !req.getEndDate().isBlank()) {
            y.setEndDate(end);
        }
        y.setActive(true);
        y.setCreatedAt(LocalDateTime.now());

        return academicYearRepo.save(y);
    }
}
