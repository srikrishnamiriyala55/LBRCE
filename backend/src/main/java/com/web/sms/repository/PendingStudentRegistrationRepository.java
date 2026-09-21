package com.web.sms.repository;

import com.web.sms.entity.PendingStudentRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PendingStudentRegistrationRepository extends JpaRepository<PendingStudentRegistration, String> {
    Optional<PendingStudentRegistration> findByRollNumberIgnoreCase(String rollNumber);
    Optional<PendingStudentRegistration> findByEmailIgnoreCase(String email);
}
