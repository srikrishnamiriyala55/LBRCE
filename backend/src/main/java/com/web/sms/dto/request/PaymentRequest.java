package com.web.sms.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class PaymentRequest {
    @NotNull(message = "Fee ID is required")
    @Positive(message = "Fee ID must be greater than zero")
    private Long feeId;
    @NotNull(message = "Payment amount is required")
    @Positive(message = "Payment amount must be at least ₹1")
    private Long amount;

    public Long getFeeId() { return feeId; }
    public void setFeeId(Long feeId) { this.feeId = feeId; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
}
