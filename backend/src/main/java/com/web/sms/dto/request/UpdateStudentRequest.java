package com.web.sms.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class UpdateStudentRequest {
    @NotBlank @Size(max=100) private String name;
    @NotBlank @Email @Size(max=120) private String email;
    @NotBlank @Pattern(regexp="^[0-9]{10,15}$") private String phoneNumber;
    @NotNull @Past private LocalDate dob;
    @NotBlank @Pattern(regexp="MALE|FEMALE|OTHER") private String gender;
    @NotBlank @Size(max=500) private String address;
    @NotBlank @Size(max=50) private String branch;
    @NotNull @Min(1) @Max(4) private Integer year;
    @NotNull @Min(1) @Max(8) private Integer semester;
    @NotBlank @Pattern(regexp="A\\+|A-|B\\+|B-|AB\\+|AB-|O\\+|O-") private String bloodGroup;
    @NotBlank @Size(max=100) private String parentName;
    @NotBlank @Pattern(regexp="^[0-9]{10,15}$") private String parentPhoneNumber;

    public String getName(){return name;} public void setName(String v){name=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPhoneNumber(){return phoneNumber;} public void setPhoneNumber(String v){phoneNumber=v;}
    public LocalDate getDob(){return dob;} public void setDob(LocalDate v){dob=v;}
    public String getGender(){return gender;} public void setGender(String v){gender=v;}
    public String getAddress(){return address;} public void setAddress(String v){address=v;}
    public String getBranch(){return branch;} public void setBranch(String v){branch=v;}
    public Integer getYear(){return year;} public void setYear(Integer v){year=v;}
    public Integer getSemester(){return semester;} public void setSemester(Integer v){semester=v;}
    public String getBloodGroup(){return bloodGroup;} public void setBloodGroup(String v){bloodGroup=v;}
    public String getParentName(){return parentName;} public void setParentName(String v){parentName=v;}
    public String getParentPhoneNumber(){return parentPhoneNumber;} public void setParentPhoneNumber(String v){parentPhoneNumber=v;}
}
