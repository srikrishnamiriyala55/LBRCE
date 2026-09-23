package com.web.sms.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class CreateStudentRequest {
    @NotBlank @Size(max=30) private String rollNumber;
    @NotBlank @Size(max=100) private String name;
    @NotBlank @Email @Size(max=120) private String email;
    @NotBlank @Pattern(regexp="^[0-9]{10,15}$") private String phoneNumber;
    @NotBlank @Size(min=8,max=72) private String password;
    @NotBlank @Size(max=50) private String branch;
    @NotNull @Min(1) @Max(4) private Integer year;
    @NotNull @Min(1) @Max(8) private Integer semester;
    @NotBlank @Pattern(regexp="MALE|FEMALE|OTHER") private String gender;
    @NotBlank @Size(max=500) private String address;
    @NotNull @Past private LocalDate dob;
    @NotBlank @Pattern(regexp="A\\+|A-|B\\+|B-|AB\\+|AB-|O\\+|O-") private String bloodGroup;
    @NotBlank @Size(max=100) private String parentName;
    @NotBlank @Pattern(regexp="^[0-9]{10,15}$") private String parentPhoneNumber;

    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }
    public String getParentPhoneNumber() { return parentPhoneNumber; }
    public void setParentPhoneNumber(String parentPhoneNumber) { this.parentPhoneNumber = parentPhoneNumber; }
}
