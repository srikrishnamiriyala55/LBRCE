package com.web.sms.dto.response;

import com.web.sms.entity.Student;

public class StudentProfileResponse {
    private Long id;
    private String rollNumber;
    private String name;
    private String email;
    private String phoneNumber;
    private String dob;
    private String gender;
    private String address;
    private String branch;
    private Integer year;
    private Integer semester;
    private String bloodGroup;
    private String status;
    private String parentName;
    private String parentPhoneNumber;
    private String emergencyContact;

    public static StudentProfileResponse fromStudent(Student s) {
        StudentProfileResponse r = new StudentProfileResponse();
        r.setId(s.getId());
        r.setRollNumber(s.getRollNumber());
        r.setName(s.getName());
        r.setEmail(s.getEmail());
        r.setPhoneNumber(s.getPhoneNumber());
        if(s.getDob() != null) r.setDob(s.getDob().toString());
        r.setGender(s.getGender());
        r.setAddress(s.getAddress());
        r.setBranch(s.getBranch());
        r.setYear(s.getYear());
        r.setSemester(s.getSemester());
        r.setBloodGroup(s.getBloodGroup());
        r.setStatus(s.getStatus());
        r.setParentName(s.getParentName());
        r.setParentPhoneNumber(s.getParentPhoneNumber());
        r.setEmergencyContact(s.getEmergencyContact());
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }
    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }
    public String getParentPhoneNumber() { return parentPhoneNumber; }
    public void setParentPhoneNumber(String parentPhoneNumber) { this.parentPhoneNumber = parentPhoneNumber; }
    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
}
