package com.web.sms.service;

import com.web.sms.dto.request.PaymentRequest;
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
import com.web.sms.exception.ForbiddenException;
import com.web.sms.exception.ResourceNotFoundException;
import com.web.sms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FeeService {

    @Value("${btms.payments.auto-confirm}")
    private boolean autoConfirmPayments;

    private final FeeRepository feeRepo;
    private final PaymentRepository paymentRepo;
    private final BusPassRepository passRepo;
    private final StudentRepository studentRepo;
    private final NotificationService notificationService;

    public FeeService(FeeRepository feeRepo,
                      PaymentRepository paymentRepo,
                      BusPassRepository passRepo,
                      StudentRepository studentRepo,
                      NotificationService notificationService) {
        this.feeRepo = feeRepo;
        this.paymentRepo = paymentRepo;
        this.passRepo = passRepo;
        this.studentRepo = studentRepo;
        this.notificationService = notificationService;
    }

    public List<FeeResponse> getStudentFees(Long studentId) {
        return feeRepo.findByStudentId(studentId).stream()
                .map(FeeResponse::fromFee)
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentResponse processPayment(Long studentId, PaymentRequest req) {
        Fee fee = feeRepo.findByIdForUpdate(req.getFeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Fee record not found"));

        if (!fee.getStudent().getId().equals(studentId)) {
            throw new ForbiddenException("Unauthorized fee payment attempt");
        }
        if (fee.getAllocation() == null || fee.getAllocation().getStatus() != com.web.sms.enums.EntityStatus.ACTIVE) {
            throw new BadRequestException("Payments are allowed only for an active approved bus allocation");
        }

        if (req.getAmount() == null || req.getAmount() <= 0) {
            throw new BadRequestException("Payment amount must be greater than zero");
        }

        long remainingAmount = fee.getTotalAmount() - (fee.getPaidAmount() != null ? fee.getPaidAmount() : 0L);
        if (remainingAmount <= 0 || "PAID".equalsIgnoreCase(fee.getStatus())) {
            throw new BadRequestException("This fee has already been paid in full");
        }
        if (paymentRepo.existsByFeeIdAndStatus(fee.getId(), PaymentStatus.INITIATED)) {
            throw new BadRequestException("A payment is already in progress for this fee");
        }
        if (req.getAmount() > remainingAmount) {
            throw new BadRequestException("Payment amount cannot exceed remaining balance of ₹" + remainingAmount);
        }

        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        Payment payment = new Payment();
        payment.setFee(fee);
        payment.setStudent(student);
        payment.setAmount(req.getAmount());
        payment.setOrderId("ORD_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        payment.setStatus(PaymentStatus.INITIATED);
        payment.setGatewayName("READY_GATEWAY");
        payment.setCreatedAt(LocalDateTime.now());

        Payment saved = paymentRepo.save(payment);
        if (autoConfirmPayments) {
            String txnId = "TXN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
            confirmPayment(saved.getOrderId(), txnId, "LOCAL_DEMO_SUCCESS");
            saved = paymentRepo.findById(saved.getId()).orElse(saved);
        }
        return PaymentResponse.fromPayment(saved);
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

        notificationService.createNotification(
                fee.getStudent().getRollNumber(),
                "Payment Successful",
                "Payment of ₹" + payment.getAmount() + " processed successfully. Order ID: " + orderId,
                "PAYMENT"
        );
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

            if (!passExists && fee.getAllocation() != null) {
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
