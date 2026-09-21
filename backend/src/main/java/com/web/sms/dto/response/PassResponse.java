package com.web.sms.dto.response;

import com.web.sms.entity.BusPass;
import com.web.sms.enums.PassStatus;
import java.time.LocalDate;

public class PassResponse {
    private Long id;
    private String passNumber;
    private String studentName;
    private String rollNumber;
    private String branch;
    private Integer year;
    private Integer semester;
    private String phoneNumber;
    private String busNumber;
    private String routeName;
    private String boardingPoint;
    private String academicYear;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private PassStatus status;
    private String verificationToken;
    private String qrCodePath;
    private boolean photoAvailable;

    public static PassResponse fromBusPass(BusPass pass) {
        PassResponse r = new PassResponse();
        r.setId(pass.getId());
        r.setPassNumber(pass.getPassNumber());
        if(pass.getStudent() != null) {
            r.setStudentName(pass.getStudent().getName());
            r.setRollNumber(pass.getStudent().getRollNumber());
            r.setBranch(pass.getStudent().getBranch());
            r.setYear(pass.getStudent().getYear());
            r.setSemester(pass.getStudent().getSemester());
            r.setPhoneNumber(pass.getStudent().getPhoneNumber());
            r.setPhotoAvailable(pass.getStudent().getPhotoData() != null && pass.getStudent().getPhotoData().length > 0);
        }
        if(pass.getAllocation() != null) {
            if(pass.getAllocation().getBus() != null) {
                com.web.sms.entity.Bus bus = pass.getAllocation().getBus();
                r.setBusNumber(bus.getBusNumber());
                if(bus.getRoute() != null && bus.getRoute().getRouteName() != null
                        && !bus.getRoute().getRouteName().isBlank()) {
                    r.setRouteName(bus.getRoute().getRouteName());
                } else if(bus.getStartingPoint() != null && !bus.getStartingPoint().isBlank()
                        && bus.getEndingPoint() != null && !bus.getEndingPoint().isBlank()) {
                    r.setRouteName(bus.getStartingPoint() + " - " + bus.getEndingPoint());
                } else if(bus.getStartingPoint() != null && !bus.getStartingPoint().isBlank()) {
                    r.setRouteName(bus.getStartingPoint());
                } else if(bus.getEndingPoint() != null && !bus.getEndingPoint().isBlank()) {
                    r.setRouteName(bus.getEndingPoint());
                }
            }
            if(pass.getAllocation().getBoardingPoint() != null) {
                r.setBoardingPoint(pass.getAllocation().getBoardingPoint().getStationName());
            }
        }
        if(pass.getAcademicYear() != null) r.setAcademicYear(pass.getAcademicYear().getYearName());
        r.setValidFrom(pass.getValidFrom());
        r.setValidUntil(pass.getValidUntil());
        r.setStatus(pass.getStatus());
        r.setVerificationToken(pass.getVerificationToken());
        r.setQrCodePath(pass.getQrCodePath());
        return r;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPassNumber() { return passNumber; }
    public void setPassNumber(String passNumber) { this.passNumber = passNumber; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public String getBoardingPoint() { return boardingPoint; }
    public void setBoardingPoint(String boardingPoint) { this.boardingPoint = boardingPoint; }
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }
    public PassStatus getStatus() { return status; }
    public void setStatus(PassStatus status) { this.status = status; }
    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }
    public String getQrCodePath() { return qrCodePath; }
    public void setQrCodePath(String qrCodePath) { this.qrCodePath = qrCodePath; }
    public boolean isPhotoAvailable() { return photoAvailable; }
    public void setPhotoAvailable(boolean photoAvailable) { this.photoAvailable = photoAvailable; }
}
