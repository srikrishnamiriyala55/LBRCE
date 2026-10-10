package com.web.sms.dto;

import com.web.sms.dto.response.PassResponse;
import com.web.sms.entity.*;
import com.web.sms.enums.PassStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HistoricalSnapshotResponseTest {
    @Test
    void passUsesAllocationSnapshotInsteadOfChangedMasterData(){
        Student student=new Student();student.setName("Current Name");student.setRollNumber("NEW-ROLL");student.setBranch("ECE");student.setYear(4);student.setSemester(8);student.setPhoneNumber("9999999999");
        Bus bus=new Bus();bus.setBusNumber("NEW-BUS");bus.setStartingPoint("New Start");bus.setEndingPoint("New End");
        BoardingPoints point=new BoardingPoints();point.setStationName("New Point");
        TransportAllocation allocation=new TransportAllocation();allocation.setStudent(student);allocation.setBus(bus);allocation.setBoardingPoint(point);
        allocation.setStudentNameSnapshot("Historical Name");allocation.setRollNumberSnapshot("OLD-ROLL");allocation.setBranchSnapshot("CSE");allocation.setStudyYearSnapshot(2);allocation.setSemesterSnapshot(3);allocation.setPhoneNumberSnapshot("9000000000");allocation.setBusNumberSnapshot("BUS101");allocation.setStartingPointSnapshot("Old Start");allocation.setEndingPointSnapshot("LBRCE");allocation.setBoardingPointSnapshot("Old Point");
        AcademicYear year=new AcademicYear();year.setYearName("2025-26");
        BusPass pass=new BusPass();pass.setId(1L);pass.setStudent(student);pass.setAllocation(allocation);pass.setAcademicYear(year);pass.setPassNumber("PASS-1");pass.setVerificationToken("token");pass.setStatus(PassStatus.EXPIRED);

        PassResponse response=PassResponse.fromBusPass(pass);
        assertEquals("Historical Name",response.getStudentName());
        assertEquals("OLD-ROLL",response.getRollNumber());
        assertEquals("CSE",response.getBranch());
        assertEquals(2,response.getYear());
        assertEquals(3,response.getSemester());
        assertEquals("BUS101",response.getBusNumber());
        assertEquals("Old Point",response.getBoardingPoint());
    }
}
