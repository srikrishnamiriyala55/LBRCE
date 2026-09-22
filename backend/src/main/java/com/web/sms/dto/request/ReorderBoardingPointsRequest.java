package com.web.sms.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class ReorderBoardingPointsRequest {
    @NotEmpty(message = "At least one boarding point is required")
    private List<@NotNull Long> boardingPointIds;

    public List<Long> getBoardingPointIds() { return boardingPointIds; }
    public void setBoardingPointIds(List<Long> boardingPointIds) { this.boardingPointIds = boardingPointIds; }
}
