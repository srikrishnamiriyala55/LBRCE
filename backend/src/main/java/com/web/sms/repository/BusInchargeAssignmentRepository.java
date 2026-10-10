package com.web.sms.repository;

import com.web.sms.entity.BusInchargeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BusInchargeAssignmentRepository extends JpaRepository<BusInchargeAssignment,Long> {
    Optional<BusInchargeAssignment> findByBusIdAndAcademicYearIdAndStatus(Long busId,Long academicYearId,String status);
    Optional<BusInchargeAssignment> findByBusIdAndInchargeIdAndAcademicYearId(Long busId,Long inchargeId,Long academicYearId);
    Optional<BusInchargeAssignment> findFirstByInchargeIdAndAcademicYearIdOrderByAssignedAtDesc(Long inchargeId,Long academicYearId);
    List<BusInchargeAssignment> findByBusIdAndStatus(Long busId,String status);
    List<BusInchargeAssignment> findByInchargeId(Long inchargeId);
}
