package com.web.sms.dto.request;

import jakarta.validation.constraints.NotNull;

public class BusApplicationRequest {
    @NotNull
    private Long busId;
    @NotNull
    private Long boardingPointId;
    private Long academicYearId;

    public Long getBusId() { return busId; }
    public void setBusId(Long busId) { this.busId = busId; }
    public Long getBoardingPointId() { return boardingPointId; }
    public void setBoardingPointId(Long boardingPointId) { this.boardingPointId = boardingPointId; }
    public Long getAcademicYearId() { return academicYearId; }
    public void setAcademicYearId(Long academicYearId) { this.academicYearId = academicYearId; }
}
