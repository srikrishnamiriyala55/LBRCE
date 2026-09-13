package com.web.sms.dto.request;

import jakarta.validation.constraints.NotBlank;

public class CreateAcademicYearRequest {
    @NotBlank
    private String yearName;
    private String startDate;
    private String endDate;

    public String getYearName() { return yearName; }
    public void setYearName(String yearName) { this.yearName = yearName; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
}
