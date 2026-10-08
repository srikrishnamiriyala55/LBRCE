package com.web.sms.service;

import com.web.sms.dto.request.CreateAcademicYearRequest;
import com.web.sms.dto.request.CreateStudentRequest;
import com.web.sms.dto.request.UpdateStudentRequest;
import com.web.sms.dto.request.StudentAcademicDetailsRequest;
import com.web.sms.dto.response.DashboardResponse;
import com.web.sms.dto.response.StudentProfileResponse;
import com.web.sms.entity.AcademicYear;
import com.web.sms.entity.Bus;
import com.web.sms.entity.Student;
import com.web.sms.entity.TransportAllocation;
import com.web.sms.enums.ApplicationStatus;
import com.web.sms.enums.EntityStatus;
import com.web.sms.enums.PassStatus;
import com.web.sms.enums.TransferStatus;
import com.web.sms.exception.BadRequestException;
import com.web.sms.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.io.IOException;
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
    private final PaymentRepository paymentRepo;
    private final NotificationRepository notificationRepo;
    private final AuditService auditService;
    private final StudentAcademicDetailsRepository studentAcademicDetailsRepo;
    private final StudentAcademicDetailsService studentAcademicDetailsService;
    private final NotificationService notificationService;

    public AdminService(StudentRepository studentRepo,
                        BusRepository busRepo,
                        InchargeRepository inchargeRepo,
                        BusApplicationRepository applicationRepo,
                        BusPassRepository passRepo,
                        FeeRepository feeRepo,
                        TransportAllocationRepository allocationRepo,
                        AcademicYearRepository academicYearRepo,
                        PasswordEncoder passwordEncoder, TransferRequestRepository transferRepo,
                        PaymentRepository paymentRepo, NotificationRepository notificationRepo,
                        AuditService auditService, StudentAcademicDetailsRepository studentAcademicDetailsRepo,
                        StudentAcademicDetailsService studentAcademicDetailsService,
                        NotificationService notificationService) {
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
        this.paymentRepo = paymentRepo;
        this.notificationRepo = notificationRepo;
        this.auditService = auditService;
        this.studentAcademicDetailsRepo = studentAcademicDetailsRepo;
        this.studentAcademicDetailsService = studentAcademicDetailsService;
        this.notificationService = notificationService;
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
        long totalCapacity = busRepo.sumActiveCapacity();

        // Scope operational metrics to the active academic year when one exists
        java.util.Optional<com.web.sms.entity.AcademicYear> activeYearOpt = academicYearRepo.findByActiveTrue();
        final Long activeYearId = activeYearOpt.map(com.web.sms.entity.AcademicYear::getId).orElse(null);
        res.put("activeAcademicYear", activeYearOpt.map(com.web.sms.entity.AcademicYear::getYearName).orElse("—"));

        long pendingApplications = activeYearId != null
                ? applicationRepo.findAllForAdminList().stream()
                        .filter(a -> a.getAcademicYear() != null && a.getAcademicYear().getId().equals(activeYearId)
                                && a.getStatus() == ApplicationStatus.PENDING).count()
                : applicationRepo.countByStatus(ApplicationStatus.PENDING);

        long occupiedSeats = activeYearId != null
                ? allocationRepo.findByAcademicYearIdAndStatus(activeYearId, EntityStatus.ACTIVE).size()
                : allocationRepo.countByStatus(EntityStatus.ACTIVE);

        long passesGenerated = activeYearId != null
                ? passRepo.findByAcademicYearIdAndStatus(activeYearId, PassStatus.ACTIVE).size()
                : passRepo.countByStatus(PassStatus.ACTIVE);

        Long totalFeeCollection = activeYearId != null
                ? feeRepo.findAll().stream()
                        .filter(f -> f.getAcademicYear() != null && f.getAcademicYear().getId().equals(activeYearId))
                        .mapToLong(f -> f.getPaidAmount() != null ? f.getPaidAmount() : 0L).sum()
                : feeRepo.sumTotalPaidAmount();

        long vacancy = Math.max(0, totalCapacity - occupiedSeats);

        // Renewal pending: students who had an allocation in any previous year but have no allocation or
        // active application in the current year
        long renewalPending = 0;
        if (activeYearId != null) {
            java.util.Set<Long> allocatedThisYear = allocationRepo.findByAcademicYearIdAndStatus(activeYearId, EntityStatus.ACTIVE)
                    .stream().map(a -> a.getStudent().getId()).collect(java.util.stream.Collectors.toSet());
            java.util.Set<Long> appliedThisYear = applicationRepo.findAllForAdminList().stream()
                    .filter(a -> a.getAcademicYear() != null && a.getAcademicYear().getId().equals(activeYearId)
                            && (a.getStatus() == ApplicationStatus.PENDING || a.getStatus() == ApplicationStatus.UNDER_REVIEW
                                    || a.getStatus() == ApplicationStatus.APPROVED))
                    .map(a -> a.getStudent().getId()).collect(java.util.stream.Collectors.toSet());
            // Students with any allocation in the system that is NOT in the current year
            java.util.Set<Long> hadPreviousAllocation = allocationRepo.findAll().stream()
                    .filter(a -> !a.getAcademicYear().getId().equals(activeYearId))
                    .map(a -> a.getStudent().getId()).collect(java.util.stream.Collectors.toSet());
            renewalPending = hadPreviousAllocation.stream()
                    .filter(sId -> !allocatedThisYear.contains(sId) && !appliedThisYear.contains(sId)).count();
        }
        res.put("renewalPending", renewalPending);

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
        res.put("studentsWithoutTransportation", Math.max(0, totalStudents - occupiedSeats));
        res.put("approvedApplications", activeYearId != null
                ? applicationRepo.findAllForAdminList().stream()
                        .filter(a -> a.getAcademicYear() != null && a.getAcademicYear().getId().equals(activeYearId)
                                && a.getStatus() == ApplicationStatus.APPROVED).count()
                : applicationRepo.countByStatus(ApplicationStatus.APPROVED));
        res.put("rejectedApplications", activeYearId != null
                ? applicationRepo.findAllForAdminList().stream()
                        .filter(a -> a.getAcademicYear() != null && a.getAcademicYear().getId().equals(activeYearId)
                                && a.getStatus() == ApplicationStatus.REJECTED).count()
                : applicationRepo.countByStatus(ApplicationStatus.REJECTED));
        res.put("inactiveBuses", totalBuses - activeBuses);
        res.put("fullBuses", busRepo.countFullBuses());
        res.put("activeIncharges", inchargeRepo.countByStatusIgnoreCase("ACTIVE"));
        res.put("inactiveIncharges", inchargeRepo.countByStatusIgnoreCase("INACTIVE"));
        res.put("transferRequested", transferRepo.countByStatus(TransferStatus.TRANSFER_REQUESTED));
        res.put("waitingNewIncharge", transferRepo.countByStatus(TransferStatus.OLD_INCHARGE_APPROVED));
        res.put("completedTransfers", transferRepo.countByStatus(TransferStatus.TRANSFER_COMPLETED));
        res.put("rejectedTransfers", transferRepo.countByStatus(TransferStatus.OLD_INCHARGE_REJECTED)
                + transferRepo.countByStatus(TransferStatus.NEW_INCHARGE_REJECTED));
        long expected = activeYearId != null
                ? feeRepo.findAll().stream()
                        .filter(f -> f.getAcademicYear() != null && f.getAcademicYear().getId().equals(activeYearId))
                        .mapToLong(f -> f.getTotalAmount() != null ? f.getTotalAmount() : 0L).sum()
                : feeRepo.sumTotalExpected();
        res.put("totalExpectedFees", expected);
        res.put("pendingFeeAmount", Math.max(0, expected - (totalFeeCollection == null ? 0 : totalFeeCollection)));
        res.put("passEligibleStudents", activeYearId != null
                ? feeRepo.findAll().stream()
                        .filter(f -> f.getAcademicYear() != null && f.getAcademicYear().getId().equals(activeYearId)
                                && f.isPassEligible()).count()
                : feeRepo.countByPassEligibleTrue());
        res.put("expiredOrRevokedPasses", passRepo.countByStatus(PassStatus.EXPIRED)
                + passRepo.countByStatus(PassStatus.REVOKED) + passRepo.countByStatus(PassStatus.INACTIVE));
        return res;
    }

    @Transactional
    public StudentProfileResponse createStudent(CreateStudentRequest req, MultipartFile photo) {
        String rollNumber=req.getRollNumber().trim().toUpperCase();
        String email=req.getEmail().trim().toLowerCase();
        if (studentRepo.existsByRollNumber(rollNumber)) {
            throw new BadRequestException("Student with roll number " + rollNumber + " already exists");
        }
        if (studentRepo.existsByEmail(email)) {
            throw new BadRequestException("Student with email " + email + " already exists");
        }
        validateAcademicProgress(req.getBranch(),req.getYear(),req.getSemester());

        Student s = new Student();
        s.setRollNumber(rollNumber);
        s.setName(req.getName().trim());
        s.setEmail(email);
        s.setPhoneNumber(req.getPhoneNumber().trim());
        s.setPassword(passwordEncoder.encode(req.getPassword()));
        s.setBranch(req.getBranch().trim().toUpperCase());
        s.setYear(req.getYear());
        s.setSemester(req.getSemester());
        s.setGender(req.getGender());
        s.setAddress(req.getAddress().trim());
        s.setDob(req.getDob());
        s.setBloodGroup(req.getBloodGroup());
        s.setParentName(req.getParentName().trim());
        s.setParentPhoneNumber(req.getParentPhoneNumber().trim());
        storePhoto(s,photo,true);
        s.setStatus("ACTIVE");
        LocalDateTime now=LocalDateTime.now();s.setCreatedAt(now);s.setUpdatedAt(now);

        Student saved = studentRepo.save(s);
        AcademicYear activeYear = academicYearRepo.findByActiveTrue().orElseThrow(() -> new BadRequestException("Create an active academic year before adding students"));
        return StudentProfileResponse.fromStudent(saved, studentAcademicDetailsService.upsert(saved, activeYear, req.getBranch(), req.getYear(), req.getSemester()));
    }

    @Transactional
    public StudentProfileResponse updateStudent(Long studentId, UpdateStudentRequest req, MultipartFile photo, String actor) {
        Student student=studentRepo.findByIdForUpdate(studentId).orElseThrow(()->new com.web.sms.exception.ResourceNotFoundException("Student not found"));
        validateAcademicProgress(req.getBranch(),req.getYear(),req.getSemester());
        String email=req.getEmail().trim().toLowerCase();
        String rollNumber=req.getRollNumber().trim().toUpperCase();
        if (!rollNumber.equals(student.getRollNumber()) && studentRepo.existsByRollNumber(rollNumber)) throw new BadRequestException("Student with roll number " + rollNumber + " already exists");
        if(studentRepo.existsByEmailAndIdNot(email,studentId))throw new BadRequestException("Another student account already uses this email address");
        student.setRollNumber(rollNumber);
        applyStudentDetails(student,req,email);
        storePhoto(student,photo,false);
        Student saved=studentRepo.save(student);
        academicYearRepo.findByActiveTrue().ifPresent(year -> studentAcademicDetailsService.upsert(saved, year, req.getBranch(), req.getYear(), req.getSemester()));
        auditService.log(actor,"ADMIN","STUDENT_UPDATED","STUDENT",String.valueOf(studentId),null,saved.getRollNumber(),null);
        return StudentProfileResponse.fromStudent(saved);
    }

    @Transactional
    public StudentProfileResponse updateStudentForIncharge(Long studentId, Long busId, UpdateStudentRequest req, MultipartFile photo, String actor) {
        TransportAllocation allocation=allocationRepo.findByStudentIdAndBusIdAndStatus(studentId,busId,EntityStatus.ACTIVE)
                .orElseThrow(()->new com.web.sms.exception.ForbiddenException("Student is not actively assigned to your bus"));
        Student student=studentRepo.findByIdForUpdate(allocation.getStudent().getId()).orElseThrow(()->new com.web.sms.exception.ResourceNotFoundException("Student not found"));
        validateAcademicProgress(req.getBranch(),req.getYear(),req.getSemester());
        String email=req.getEmail().trim().toLowerCase();
        if(studentRepo.existsByEmailAndIdNot(email,studentId))throw new BadRequestException("Another student account already uses this email address");
        applyStudentDetails(student,req,email);
        storePhoto(student,photo,false);
        Student saved=studentRepo.save(student);
        academicYearRepo.findByActiveTrue().ifPresent(year -> studentAcademicDetailsService.upsert(saved, year, req.getBranch(), req.getYear(), req.getSemester()));
        auditService.log(actor,"INCHARGE","STUDENT_UPDATED","STUDENT",String.valueOf(studentId),null,saved.getRollNumber(),"Assigned bus: "+allocation.getBus().getBusNumber());
        return StudentProfileResponse.fromStudent(saved);
    }

    @Transactional
    public void deleteStudent(Long studentId,String actor){
        Student student=studentRepo.findByIdForUpdate(studentId).orElseThrow(()->new com.web.sms.exception.ResourceNotFoundException("Student not found"));
        List<Bus> affected=allocationRepo.findByStudentId(studentId).stream().map(TransportAllocation::getBus).filter(java.util.Objects::nonNull).distinct().toList();
        paymentRepo.deleteByStudentId(studentId);passRepo.deleteByStudentId(studentId);feeRepo.deleteByStudentId(studentId);
        transferRepo.deleteByStudentId(studentId);allocationRepo.deleteByStudentId(studentId);applicationRepo.deleteByStudentId(studentId);
        notificationRepo.deleteByRecipientRollNumber(student.getRollNumber());studentAcademicDetailsRepo.deleteByStudentId(studentId);studentRepo.delete(student);studentRepo.flush();
        affected.forEach(bus->{long occupied=allocationRepo.countByBusIdAndStatus(bus.getId(),EntityStatus.ACTIVE);bus.setAvailableSeats(Math.max(0,bus.getTotalSeats()-(int)occupied));busRepo.save(bus);});
        auditService.log(actor,"ADMIN","STUDENT_DELETED","STUDENT",String.valueOf(studentId),student.getRollNumber(),null,"Related applications, allocations, fees, payments, passes, transfers and notifications removed");
    }

    private void applyStudentDetails(Student s,UpdateStudentRequest req,String email){s.setName(req.getName().trim());s.setEmail(email);s.setPhoneNumber(req.getPhoneNumber().trim());s.setDob(req.getDob());s.setGender(req.getGender());s.setAddress(req.getAddress().trim());s.setBranch(req.getBranch().trim().toUpperCase());s.setYear(req.getYear());s.setSemester(req.getSemester());s.setBloodGroup(req.getBloodGroup());s.setParentName(req.getParentName().trim());s.setParentPhoneNumber(req.getParentPhoneNumber().trim());}
    private void validateAcademicProgress(String department,Integer year,Integer semester){if(year==null||semester==null||(semester+1)/2!=year)throw new BadRequestException("Semester must correspond to the selected study year");if("MBA".equalsIgnoreCase(department)&&year>2)throw new BadRequestException("MBA is a two-year programme; select year 1 or 2");}
    private void storePhoto(Student student,MultipartFile photo,boolean required){if(photo==null||photo.isEmpty()){if(required)throw new BadRequestException("A student photo is required");return;}if(photo.getSize()>2L*1024*1024)throw new BadRequestException("Student photo must not exceed 2 MB");try{byte[] bytes=photo.getBytes();boolean jpeg=bytes.length>=3&&(bytes[0]&0xff)==0xff&&(bytes[1]&0xff)==0xd8&&(bytes[2]&0xff)==0xff;boolean png=bytes.length>=8&&(bytes[0]&0xff)==0x89&&bytes[1]==0x50&&bytes[2]==0x4e&&bytes[3]==0x47;if(!jpeg&&!png)throw new BadRequestException("Student photo must be a valid JPEG or PNG image");student.setPhotoData(bytes);student.setPhotoContentType(jpeg?MediaType.IMAGE_JPEG_VALUE:MediaType.IMAGE_PNG_VALUE);}catch(IOException e){throw new BadRequestException("Unable to read the student photo");}}

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
        // A rollover closes operational access for the old year. Students keep
        // their accounts but must submit a new application in the new year.
        AcademicYear current = academicYearRepo.findByActiveTrue().orElse(null);
        java.util.List<String> renewalRollNumbers = new java.util.ArrayList<>();
        if (current != null) {
            long pending = applicationRepo.findAllForAdminList().stream()
                    .filter(a -> a.getAcademicYear().getId().equals(current.getId()))
                    .filter(a -> a.getStatus() == ApplicationStatus.PENDING || a.getStatus() == ApplicationStatus.UNDER_REVIEW).count();
            if (pending > 0) throw new BadRequestException("Resolve " + pending + " pending application(s) before closing " + current.getYearName());
            LocalDateTime now = LocalDateTime.now();
            allocationRepo.findByAcademicYearIdAndStatus(current.getId(), EntityStatus.ACTIVE).forEach(a -> {
                renewalRollNumbers.add(a.getStudent().getRollNumber());
                a.setStatus(EntityStatus.INACTIVE);
                a.setDeactivatedAt(now);
            });
            passRepo.findByAcademicYearIdAndStatus(current.getId(), PassStatus.ACTIVE).forEach(p -> { p.setStatus(PassStatus.EXPIRED); p.setRevokedAt(now); });
            busRepo.findAll().forEach(bus -> { if (bus.getTotalSeats() != null) bus.setAvailableSeats(bus.getTotalSeats()); });
        }
        List<AcademicYear> allYears = academicYearRepo.findAll();
        allYears.forEach(yearItem -> yearItem.setActive(false));
        academicYearRepo.saveAll(allYears);

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

        AcademicYear saved = academicYearRepo.save(y);

        // Notify all students who had a bus allocation in the previous year to renew
        String newYearName = saved.getYearName();
        renewalRollNumbers.forEach(rollNumber ->
                notificationService.createNotification(
                        rollNumber,
                        "Bus Renewal Required for " + newYearName,
                        "A new academic year (" + newYearName + ") has been activated. Your previous bus pass has expired. " +
                        "Please renew your bus application for the new academic year. Contact the transport office if your academic details are not yet registered.",
                        "RENEWAL"
                )
        );

        return saved;
    }


    @Transactional
    public StudentProfileResponse setStudentAcademicDetails(Long studentId, Long academicYearId, StudentAcademicDetailsRequest request, String actor) {
        Student student = studentRepo.findByIdForUpdate(studentId).orElseThrow(() -> new com.web.sms.exception.ResourceNotFoundException("Student not found"));
        AcademicYear year = academicYearRepo.findById(academicYearId).orElseThrow(() -> new com.web.sms.exception.ResourceNotFoundException("Academic year not found"));
        var details = studentAcademicDetailsService.upsert(student, year, request.getBranch(), request.getYear(), request.getSemester());
        // The legacy fields are retained temporarily for compatibility; only
        // update them when editing the currently active academic year.
        if (year.isActive()) { student.setBranch(details.getBranch()); student.setYear(details.getYear()); student.setSemester(details.getSemester()); studentRepo.save(student); }
        auditService.log(actor, "ADMIN", "STUDENT_ACADEMIC_DETAILS_SET", "STUDENT", String.valueOf(studentId), null,
                year.getYearName() + " / " + details.getBranch() + " / " + details.getYear() + " / " + details.getSemester(), null);
        return StudentProfileResponse.fromStudent(student, details);
    }
}
