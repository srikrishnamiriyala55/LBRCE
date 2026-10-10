package com.web.sms.repository;

import com.web.sms.entity.StudentAcademicDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StudentAcademicDetailsRepository extends JpaRepository<StudentAcademicDetails, Long> {
    Optional<StudentAcademicDetails> findByStudentIdAndAcademicYearId(Long studentId, Long academicYearId);
    List<StudentAcademicDetails> findByStudentIdOrderByAcademicYearStartDateDesc(Long studentId);
    List<StudentAcademicDetails> findByAcademicYearId(Long academicYearId);
    void deleteByStudentId(Long studentId);
}
