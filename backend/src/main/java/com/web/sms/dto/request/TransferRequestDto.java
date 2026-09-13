package com.web.sms.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TransferRequestDto {
    @NotNull
    private Long requestedBusId;
    @NotNull
    private Long requestedBoardingPointId;
    @NotBlank
    @Size(max=500)
    private String reason;

    public Long getRequestedBusId() { return requestedBusId; }
    public void setRequestedBusId(Long requestedBusId) { this.requestedBusId = requestedBusId; }
    public Long getRequestedBoardingPointId() { return requestedBoardingPointId; }
    public void setRequestedBoardingPointId(Long requestedBoardingPointId) { this.requestedBoardingPointId = requestedBoardingPointId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
