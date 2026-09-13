package com.web.sms.repository;
import com.web.sms.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {
    Optional<AcademicYear> findByYearName(String yearName);
    Optional<AcademicYear> findByActiveTrue();
}
