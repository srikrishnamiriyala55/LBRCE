package com.web.sms.service;

import com.web.sms.dto.request.AdminManualPaymentRequest;
import com.web.sms.dto.response.FeeResponse;
import com.web.sms.entity.AcademicYear;
import com.web.sms.entity.Fee;
import com.web.sms.entity.Payment;
import com.web.sms.entity.Student;
import com.web.sms.entity.TransportAllocation;
import com.web.sms.enums.EntityStatus;
import com.web.sms.enums.PaymentStatus;
import com.web.sms.exception.BadRequestException;
import com.web.sms.repository.BusPassRepository;
import com.web.sms.repository.FeeRepository;
import com.web.sms.repository.PaymentRepository;
import com.web.sms.repository.AdminRepository;
import com.web.sms.repository.BusInchargeAssignmentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeeServiceTest {
    @Mock FeeRepository fees;
    @Mock PaymentRepository payments;
    @Mock BusPassRepository passes;
    @Mock NotificationService notifications;
    @Mock AdminRepository admins;
    @Mock BusInchargeAssignmentRepository assignments;
    @Mock ApplicationEventPublisher events;
    FeeService service;
    Fee fee;

    @BeforeEach
    void setUp() {
        service = new FeeService(fees, payments, passes, notifications, admins, assignments, events);
        lenient().when(admins.findAll()).thenReturn(java.util.List.of());
        Student student = new Student(); student.setId(7L); student.setRollNumber("STU007");
        AcademicYear year = new AcademicYear(); year.setId(2L); year.setYearName("2026-27");
        TransportAllocation allocation = new TransportAllocation(); allocation.setId(4L); allocation.setStatus(EntityStatus.ACTIVE);
        fee = new Fee(); fee.setId(3L); fee.setStudent(student); fee.setAcademicYear(year); fee.setAllocation(allocation);
        fee.setTotalAmount(16000L); fee.setPaidAmount(4000L); fee.setStatus("PARTIALLY_PAID");
    }

    @Test
    void recordsManualPaymentAtomicallyAndCreatesReceipt() {
        when(fees.findByIdForUpdate(3L)).thenReturn(Optional.of(fee));
        AdminManualPaymentRequest request = request(3000L);

        FeeResponse result = service.recordManualPayment(3L, request, "ADMIN");

        assertEquals(7000L, result.getPaidAmount());
        assertEquals("PARTIALLY_PAID", result.getStatus());
        ArgumentCaptor<Payment> payment = ArgumentCaptor.forClass(Payment.class);
        verify(payments).save(payment.capture());
        assertEquals(PaymentStatus.SUCCESS, payment.getValue().getStatus());
        assertEquals("ADMIN_MANUAL", payment.getValue().getGatewayName());
        assertTrue(payment.getValue().getOrderId().startsWith("MAN_"));
        verify(fees).save(fee);
        verify(events).publishEvent(any(PaymentReceiptEvent.class));
        verify(notifications).createNotification(eq("STU007"), anyString(), contains("₹3000"), eq("PAYMENT"));
    }

    @Test
    void rejectsPaymentAboveRemainingBalanceWithoutWriting() {
        when(fees.findByIdForUpdate(3L)).thenReturn(Optional.of(fee));

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.recordManualPayment(3L, request(12001L), "ADMIN"));

        assertTrue(error.getMessage().contains("remaining balance"));
        verifyNoInteractions(payments, notifications);
        verify(fees, never()).save(any());
    }

    @Test
    void rejectsPaymentForInactiveAllocation() {
        fee.getAllocation().setStatus(EntityStatus.INACTIVE);
        when(fees.findByIdForUpdate(3L)).thenReturn(Optional.of(fee));
        assertThrows(BadRequestException.class,
                () -> service.recordManualPayment(3L, request(1000L), "ADMIN"));
        verifyNoInteractions(payments, notifications);
    }

    @Test
    void rejectsZeroPaymentWithoutWriting() {
        when(fees.findByIdForUpdate(3L)).thenReturn(Optional.of(fee));
        BadRequestException error = assertThrows(BadRequestException.class,
                () -> service.recordManualPayment(3L, request(0L), "ADMIN"));
        assertTrue(error.getMessage().contains("at least ₹1"));
        verifyNoInteractions(payments, notifications);
        verify(fees, never()).save(any());
    }

    private AdminManualPaymentRequest request(long amount) {
        AdminManualPaymentRequest request = new AdminManualPaymentRequest();
        request.setAmount(amount); request.setReference("RCPT-101"); request.setRemarks("Cash counter");
        return request;
    }
}
