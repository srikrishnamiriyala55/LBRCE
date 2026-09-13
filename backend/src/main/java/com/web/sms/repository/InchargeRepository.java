package com.web.sms.repository;
import com.web.sms.entity.Incharge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface InchargeRepository extends JpaRepository<Incharge, Long> {
    Optional<Incharge> findByTeacherId(String teacherId);
    Optional<Incharge> findByEmail(String email);
    boolean existsByTeacherId(String teacherId);
    long countByStatusIgnoreCase(String status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Incharge i where i.id=:id") Optional<Incharge> findByIdForUpdate(Long id);
}
