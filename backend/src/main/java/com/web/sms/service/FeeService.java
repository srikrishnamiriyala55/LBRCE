package com.web.sms.service;

import com.web.sms.dto.request.AdminManualPaymentRequest;
import com.web.sms.dto.response.FeeResponse;
import com.web.sms.dto.response.PaymentResponse;
import com.web.sms.entity.AcademicYear;
import com.web.sms.entity.BusPass;
import com.web.sms.entity.Fee;
import com.web.sms.entity.Payment;
import com.web.sms.entity.Student;
import com.web.sms.enums.PassStatus;
import com.web.sms.enums.PaymentStatus;
import com.web.sms.exception.BadRequestException;
import com.web.sms.exception.ResourceNotFoundException;
import com.web.sms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FeeService {

    private final FeeRepository feeRepo;
    private final PaymentRepository paymentRepo;
    private final BusPassRepository passRepo;
    private final NotificationService notificationService;
    private final AdminRepository adminRepo;
    private final BusInchargeAssignmentRepository busInchargeAssignmentRepo;
    private final ApplicationEventPublisher eventPublisher;

    public FeeService(FeeRepository feeRepo,
                      PaymentRepository paymentRepo,
                      BusPassRepository passRepo,
                      NotificationService notificationService,
                      AdminRepository adminRepo,
                      BusInchargeAssignmentRepository busInchargeAssignmentRepo,
                      ApplicationEventPublisher eventPublisher) {
        this.feeRepo = feeRepo;
        this.paymentRepo = paymentRepo;
        this.passRepo = passRepo;
        this.notificationService = notificationService;
        this.adminRepo = adminRepo;
        this.busInchargeAssignmentRepo = busInchargeAssignmentRepo;
        this.eventPublisher = eventPublisher;
    }

    public List<FeeResponse> getStudentFees(Long studentId) {
        return feeRepo.findByStudentId(studentId).stream()
                .map(FeeResponse::fromFee)
                .collect(Collectors.toList());
    }

    @Transactional
    public FeeResponse recordManualPayment(Long feeId, AdminManualPaymentRequest req, String adminId) {
        Fee fee = feeRepo.findByIdForUpdate(feeId)
                .orElseThrow(() -> new ResourceNotFoundException("Fee record not found"));
        if (fee.getAllocation() == null || fee.getAllocation().getStatus() != com.web.sms.enums.EntityStatus.ACTIVE) {
            throw new BadRequestException("Manual payments are allowed only for an active approved bus allocation");
        }

        long paid = fee.getPaidAmount() == null ? 0L : fee.getPaidAmount();
        long remaining = fee.getTotalAmount() - paid;
        if (remaining <= 0) throw new BadRequestException("This fee has already been paid in full");
        if (req.getAmount() == null || req.getAmount() <= 0) {
            throw new BadRequestException("Payment amount must be at least ₹1");
        }
        if (req.getAmount() > remaining) {
            throw new BadRequestException("Payment amount cannot exceed remaining balance of ₹" + remaining);
        }
        if (paymentRepo.existsByFeeIdAndStatus(fee.getId(), PaymentStatus.INITIATED)) {
            throw new BadRequestException("An online payment is already in progress for this fee. Complete or reconcile it before recording another payment");
        }

        LocalDateTime now = LocalDateTime.now();
        String receipt = "MAN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        Payment payment = new Payment();
        payment.setFee(fee);
        payment.setStudent(fee.getStudent());
        payment.setAmount(req.getAmount());
        payment.setOrderId(receipt);
        payment.setTransactionId(req.getReference() == null || req.getReference().isBlank() ? receipt : req.getReference().trim());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setGatewayName("ADMIN_MANUAL");
        payment.setGatewayResponse("Recorded by " + adminId + (req.getRemarks() == null || req.getRemarks().isBlank() ? "" : ": " + req.getRemarks().trim()));
        payment.setPaidAt(now);
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        paymentRepo.save(payment);

        long newPaid = paid + req.getAmount();
        fee.setPaidAmount(newPaid);
        fee.setStatus(newPaid >= fee.getTotalAmount() ? "PAID" : "PARTIALLY_PAID");
        fee.setUpdatedAt(now);
        checkAndGeneratePass(fee);
        feeRepo.save(fee);

        notifyPaymentStakeholders(fee, req.getAmount(), receipt, "Manual fee payment recorded");
        publishReceipt(payment, fee);
        return FeeResponse.fromFee(fee);
    }

    @Transactional
    public void confirmPayment(String orderId, String transactionId, String gatewayResponse) {
        Payment payment = paymentRepo.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderId));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return;
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId(transactionId != null ? transactionId : "TXN_" + UUID.randomUUID().toString().substring(0, 12));
        payment.setGatewayResponse(gatewayResponse);
        payment.setPaidAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepo.save(payment);

        Fee fee = feeRepo.findByIdForUpdate(payment.getFee().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Fee record not found"));
        long currentPaid = fee.getPaidAmount() != null ? fee.getPaidAmount() : 0L;
        if (currentPaid + payment.getAmount() > fee.getTotalAmount()) {
            throw new BadRequestException("Payment confirmation would exceed the remaining fee balance");
        }
        long newPaid = currentPaid + payment.getAmount();
        fee.setPaidAmount(newPaid);
        fee.setUpdatedAt(LocalDateTime.now());

        if (newPaid >= fee.getTotalAmount()) {
            fee.setStatus("PAID");
        } else {
            fee.setStatus("PARTIALLY_PAID");
        }

        checkAndGeneratePass(fee);
        feeRepo.save(fee);

        notifyPaymentStakeholders(fee, payment.getAmount(), orderId, "Online fee payment successful");
        publishReceipt(payment, fee);
    }

    private void publishReceipt(Payment payment, Fee fee) {
        var allocation = fee.getAllocation();
        String busNumber = allocation == null ? null
                : allocation.getBusNumberSnapshot() != null ? allocation.getBusNumberSnapshot()
                : allocation.getBus() == null ? null : allocation.getBus().getBusNumber();
        String boardingPoint = allocation == null ? null
                : allocation.getBoardingPointSnapshot() != null ? allocation.getBoardingPointSnapshot()
                : allocation.getBoardingPoint() == null ? null : allocation.getBoardingPoint().getStationName();
        long totalPaid = fee.getPaidAmount() == null ? 0L : fee.getPaidAmount();
        long totalFee = fee.getTotalAmount() == null ? 0L : fee.getTotalAmount();
        eventPublisher.publishEvent(new PaymentReceiptEvent(
                fee.getStudent().getEmail(), fee.getStudent().getName(), fee.getStudent().getRollNumber(),
                fee.getAcademicYear() == null ? null : fee.getAcademicYear().getYearName(), busNumber, boardingPoint,
                payment.getStatus().name(), payment.getTransactionId(), payment.getOrderId(), payment.getGatewayName(),
                payment.getAmount(), totalFee, totalPaid, Math.max(0L, totalFee - totalPaid), fee.getStatus(),
                fee.isPassEligible(), payment.getPaidAt()));
    }

    private void notifyPaymentStakeholders(Fee fee, Long amount, String reference, String title) {
        String year = fee.getAcademicYear() == null ? "the selected academic year" : fee.getAcademicYear().getYearName();
        String student = fee.getStudent().getName() + " (" + fee.getStudent().getRollNumber() + ")";
        String message = "Payment of ₹" + amount + " for " + student + " in " + year
                + " was recorded successfully. Reference: " + reference;
        notificationService.createNotification(fee.getStudent().getRollNumber(), title, message, "PAYMENT");

        if (fee.getAllocation() != null && fee.getAllocation().getBus() != null && fee.getAcademicYear() != null) {
            busInchargeAssignmentRepo.findFirstByBusIdAndAcademicYearIdOrderByAssignedAtDesc(
                            fee.getAllocation().getBus().getId(), fee.getAcademicYear().getId())
                    .map(assignment -> assignment.getIncharge().getTeacherId())
                    .or(() -> java.util.Optional.ofNullable(fee.getAllocation().getBus().getIncharge())
                            .map(com.web.sms.entity.Incharge::getTeacherId))
                    .ifPresent(teacherId -> notificationService.createNotification(teacherId, title, message, "PAYMENT"));
        }

        adminRepo.findAll().stream()
                .filter(admin -> admin.getStatus() == null || "ACTIVE".equalsIgnoreCase(admin.getStatus()))
                .forEach(admin -> notificationService.createNotification(admin.getAdminId(), title, message, "PAYMENT"));
    }

    @Transactional
    public void checkAndGeneratePass(Fee fee) {
        double threshold = fee.getTotalAmount() * 0.50;
        if (fee.getPaidAmount() >= threshold) {
            fee.setPassEligible(true);

            AcademicYear year = fee.getAcademicYear();
            Student student = fee.getStudent();

            boolean passExists = passRepo
                    .findByStudentIdAndAcademicYearIdAndStatus(student.getId(), year.getId(), PassStatus.ACTIVE)
                    .isPresent();

            // Paying historical dues must never recreate an expired pass. A new pass is
            // generated only for the active year and an active allocation.
            boolean currentActiveAllocation = year != null && year.isActive()
                    && fee.getAllocation() != null
                    && fee.getAllocation().getStatus() == com.web.sms.enums.EntityStatus.ACTIVE;
            if (!passExists && currentActiveAllocation) {
                BusPass pass = new BusPass();
                pass.setStudent(student);
                pass.setAllocation(fee.getAllocation());
                pass.setAcademicYear(year);
                pass.setPassNumber("LBRCE-PASS-" + year.getYearName().replace("-", "") + "-" + student.getRollNumber());
                pass.setVerificationToken(UUID.randomUUID().toString());
                pass.setStatus(PassStatus.ACTIVE);
                pass.setValidFrom(LocalDate.now());
                pass.setValidUntil(year.getEndDate() != null ? year.getEndDate() : LocalDate.now().plusMonths(10));
                pass.setGeneratedAt(LocalDateTime.now());
                pass.setCreatedAt(LocalDateTime.now());
                passRepo.save(pass);

                notificationService.createNotification(
                        student.getRollNumber(),
                        "Digital Bus Pass Generated",
                        "Your payment has met the 50% threshold. Digital Bus Pass has been generated.",
                        "PASS"
                );
            }
        }
    }
}
