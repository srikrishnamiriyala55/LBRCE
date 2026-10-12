package com.web.sms.service;

import java.time.LocalDateTime;

public record PaymentReceiptEvent(
        String recipientEmail,
        String studentName,
        String rollNumber,
        String academicYear,
        String busNumber,
        String boardingPoint,
        String transactionStatus,
        String transactionId,
        String orderId,
        String paymentMode,
        Long amountPaid,
        Long totalFee,
        Long totalPaid,
        Long pendingFee,
        String feeStatus,
        boolean passEligible,
        LocalDateTime paidAt) {
}
