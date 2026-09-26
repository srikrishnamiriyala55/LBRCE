package com.web.sms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {
    @NotBlank
    @Size(max = 50)
    private String rollNumber;
    @NotBlank
    @Size(max = 72)
    private String password;
    
    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
