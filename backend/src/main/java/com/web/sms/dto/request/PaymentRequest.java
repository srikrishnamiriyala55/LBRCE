package com.web.sms.dto.request;

import jakarta.validation.constraints.NotNull;

public class PaymentRequest {
    @NotNull
    private Long feeId;
    @NotNull
    private Long amount;

    public Long getFeeId() { return feeId; }
    public void setFeeId(Long feeId) { this.feeId = feeId; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
}
