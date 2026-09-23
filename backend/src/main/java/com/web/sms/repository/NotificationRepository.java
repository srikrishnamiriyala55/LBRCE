package com.web.sms.repository;
import com.web.sms.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByRecipientRollNumberOrderByCreatedAtDesc(String recipientRollNumber, Pageable pageable);
    long countByRecipientRollNumberAndIsReadFalse(String recipientRollNumber);
    void deleteByRecipientRollNumber(String recipientRollNumber);
}
