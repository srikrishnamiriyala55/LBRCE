package com.web.sms.service;

import com.web.sms.dto.response.NotificationResponse;
import com.web.sms.entity.Notification;
import com.web.sms.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.web.sms.exception.ResourceNotFoundException;
import com.web.sms.exception.ForbiddenException;

import java.time.LocalDateTime;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepo;

    public NotificationService(NotificationRepository notificationRepo) {
        this.notificationRepo = notificationRepo;
    }

    @Transactional
    public void createNotification(String recipientRollNumber, String title, String message, String type) {
        if (recipientRollNumber == null || recipientRollNumber.isBlank()) return;

        Notification n = new Notification();
        n.setRecipientRollNumber(recipientRollNumber.trim().toUpperCase());
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationRepo.save(n);
    }

    public Page<NotificationResponse> getNotifications(String rollNumber, Pageable pageable) {
        return notificationRepo.findByRecipientRollNumberOrderByCreatedAtDesc(rollNumber, pageable)
                .map(NotificationResponse::fromNotification);
    }

    public long getUnreadCount(String rollNumber) {
        return notificationRepo.countByRecipientRollNumberAndIsReadFalse(rollNumber);
    }

    @Transactional
    public void markAsRead(Long notificationId, String rollNumber) {
        Notification n = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!n.getRecipientRollNumber().equalsIgnoreCase(rollNumber)) {
            throw new ForbiddenException("This notification does not belong to you");
        }
        if (!n.isRead()) {
            n.setRead(true);
            notificationRepo.save(n);
        }
    }
}
