package com.web.sms.dto.request;

import jakarta.validation.constraints.*;

public class StudentAcademicDetailsRequest {
    @NotBlank @Pattern(regexp="CSE|AI&DS|IT|CSM|ECE|EEE|MECH|CIVIL|ASE|MBA") private String branch;
    @NotNull @Min(1) @Max(4) private Integer year;
    @NotNull @Min(1) @Max(8) private Integer semester;
    public String getBranch() { return branch; } public void setBranch(String branch) { this.branch = branch; }
    public Integer getYear() { return year; } public void setYear(Integer year) { this.year = year; }
    public Integer getSemester() { return semester; } public void setSemester(Integer semester) { this.semester = semester; }
}
