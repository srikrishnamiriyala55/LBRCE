package com.web.sms.service;

import com.web.sms.entity.AcademicYear;
import com.web.sms.entity.Student;
import com.web.sms.entity.StudentAcademicDetails;
import com.web.sms.exception.BadRequestException;
import com.web.sms.repository.StudentAcademicDetailsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Centralises the invariant that academic data belongs to a year, not a permanent account. */
@Service
public class StudentAcademicDetailsService {
    private final StudentAcademicDetailsRepository repository;
    public StudentAcademicDetailsService(StudentAcademicDetailsRepository repository) { this.repository = repository; }

    @Transactional
    public StudentAcademicDetails upsert(Student student, AcademicYear academicYear, String branch, Integer year, Integer semester) {
        validate(branch, year, semester);
        StudentAcademicDetails details = repository.findByStudentIdAndAcademicYearId(student.getId(), academicYear.getId())
                .orElseGet(StudentAcademicDetails::new);
        details.setStudent(student); details.setAcademicYear(academicYear);
        details.setBranch(branch.trim().toUpperCase()); details.setYear(year); details.setSemester(semester); details.setStatus("ACTIVE");
        return repository.save(details);
    }

    public void validate(String branch, Integer year, Integer semester) {
        if (branch == null || year == null || semester == null || (semester + 1) / 2 != year)
            throw new BadRequestException("Semester must correspond to the selected study year");
        if ("MBA".equalsIgnoreCase(branch) && year > 2)
            throw new BadRequestException("MBA is a two-year programme; select year 1 or 2");
    }
}
