package com.web.sms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class StudentRegistrationRequest {
    @NotBlank(message = "Student ID is required")
    @Size(max = 30, message = "Student ID must not exceed 30 characters")
    private String rollNumber;

    @NotBlank(message = "Student name is required")
    @Size(max = 100, message = "Student name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 120, message = "Email must not exceed 120 characters")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(min = 10, max = 15, message = "Phone number must contain 10 to 15 characters")
    private String phoneNumber;

    @NotBlank(message = "Branch is required")
    @Size(max = 50, message = "Branch must not exceed 50 characters")
    private String branch;

    @Min(value = 1, message = "Year must be between 1 and 4")
    @Max(value = 4, message = "Year must be between 1 and 4")
    private Integer year;

    @Min(value = 1, message = "Semester must be between 1 and 8")
    @Max(value = 8, message = "Semester must be between 1 and 8")
    private Integer semester;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String password;

    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
