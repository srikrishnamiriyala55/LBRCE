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
    @NotBlank @Pattern(regexp="^[0-9]{10,15}$", message="Phone number must contain 10 to 15 digits")
    private String phoneNumber;
    @Size(max=72)
    private String password;
    @NotBlank @Size(max=500)
    private String address;
    @NotBlank @Size(max=50)
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
