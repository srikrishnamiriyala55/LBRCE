package com.web.sms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateInchargeRequest {
    @NotBlank @Size(max=30)
    private String teacherId;
    @NotBlank @Size(max=100)
    private String name;
    @NotBlank @Email @Size(max=120)
    private String email;
    @NotBlank @Pattern(regexp="^[0-9]{10}$", message="Phone number must contain exactly 10 digits")
    private String phoneNumber;
    @Size(max=15, message="Password must not exceed 15 characters")
    @Pattern(regexp="^$|^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9\\s]).{6,15}$", message="Password must be 6 to 15 characters and include uppercase, lowercase, number and special character")
    private String password;
    @NotBlank @Size(max=500)
    private String address;
    @NotBlank @Pattern(regexp="CSE|AI&DS|IT|CSM|ECE|EEE|MECH|CIVIL|ASE|MBA", message="Select a valid department")
    private String department;
    @NotBlank @Size(max=100)
    private String designation;

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
}
