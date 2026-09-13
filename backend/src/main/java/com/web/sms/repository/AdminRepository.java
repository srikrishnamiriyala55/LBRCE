package com.web.sms.repository;
import com.web.sms.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByAdminId(String adminId);
    boolean existsByAdminId(String adminId);
}
