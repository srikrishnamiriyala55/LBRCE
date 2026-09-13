package com.web.sms.service;

import com.web.sms.dto.request.ComplaintRequest;
import com.web.sms.dto.response.ComplaintResponse;
import com.web.sms.entity.Bus;
import com.web.sms.entity.Complaint;
import com.web.sms.entity.Student;
import com.web.sms.enums.ComplaintCategory;
import com.web.sms.enums.ComplaintStatus;
import com.web.sms.exception.BadRequestException;
import com.web.sms.exception.ResourceNotFoundException;
import com.web.sms.repository.BusRepository;
import com.web.sms.repository.ComplaintRepository;
import com.web.sms.repository.StudentRepository;
import com.web.sms.repository.AcademicYearRepository;
import com.web.sms.repository.TransportAllocationRepository;
import com.web.sms.entity.TransportAllocation;
import com.web.sms.entity.AcademicYear;
import com.web.sms.enums.EntityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepo;
    private final StudentRepository studentRepo;
    private final BusRepository busRepo;
    private final AcademicYearRepository academicYearRepo;
    private final TransportAllocationRepository allocationRepo;

    public ComplaintService(ComplaintRepository complaintRepo, StudentRepository studentRepo, BusRepository busRepo,
                            AcademicYearRepository academicYearRepo, TransportAllocationRepository allocationRepo) {
        this.complaintRepo = complaintRepo;
        this.studentRepo = studentRepo;
        this.busRepo = busRepo;
        this.academicYearRepo = academicYearRepo;
        this.allocationRepo = allocationRepo;
    }

    @Transactional
    public ComplaintResponse submitComplaint(Long studentId, ComplaintRequest req) {
        Student student = studentRepo.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        if (!"ACTIVE".equalsIgnoreCase(student.getStatus())) {
            throw new BadRequestException("Only active students can submit complaints");
        }
        AcademicYear year = academicYearRepo.findByActiveTrue()
                .orElseThrow(() -> new BadRequestException("No active academic year found"));
        TransportAllocation allocation = allocationRepo.findActiveForUpdate(studentId, year.getId())
                .orElseThrow(() -> new BadRequestException("You can submit a complaint only after your bus application is approved and a bus is allocated"));

        String subject = req.getSubject().trim();
        if (complaintRepo.existsByStudentIdAndStatusInAndSubjectIgnoreCase(studentId,
                java.util.List.of(ComplaintStatus.OPEN, ComplaintStatus.IN_PROGRESS), subject)) {
            throw new BadRequestException("You already have an open complaint with this subject");
        }

        Complaint complaint = new Complaint();
        complaint.setStudent(student);
        Bus allocatedBus = allocation.getBus();
        if (req.getBusId() != null && !req.getBusId().equals(allocatedBus.getId())) {
            throw new BadRequestException("Complaints can only be submitted for your currently allocated bus");
        }
        complaint.setBus(allocatedBus);

        try {
            complaint.setCategory(ComplaintCategory.valueOf(req.getCategory().toUpperCase()));
        } catch (IllegalArgumentException e) {
            complaint.setCategory(ComplaintCategory.OTHER);
        }

        complaint.setSubject(subject);
        complaint.setDescription(req.getDescription().trim());
        complaint.setStatus(ComplaintStatus.OPEN);
        complaint.setCreatedAt(LocalDateTime.now());

        Complaint saved = complaintRepo.save(complaint);
        return ComplaintResponse.fromComplaint(saved);
    }

    public Page<ComplaintResponse> getStudentComplaints(Long studentId, Pageable pageable) {
        return complaintRepo.findByStudentId(studentId, pageable)
                .map(ComplaintResponse::fromComplaint);
    }

    public Page<ComplaintResponse> getComplaintsByBus(Long busId, Pageable pageable) {
        return complaintRepo.findByBusId(busId, pageable)
                .map(ComplaintResponse::fromComplaint);
    }

    @Transactional
    public ComplaintResponse updateComplaintStatus(Long complaintId, String status, String response, String respondedBy) {
        return updateComplaintStatus(complaintId, status, response, respondedBy, null);
    }

    @Transactional
    public ComplaintResponse updateComplaintStatus(Long complaintId, String status, String response, String respondedBy, Long authorizedBusId) {
        Complaint complaint = complaintRepo.findByIdForUpdate(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));
        if (authorizedBusId != null && (complaint.getBus() == null || !complaint.getBus().getId().equals(authorizedBusId))) {
            throw new com.web.sms.exception.ForbiddenException("This complaint does not belong to your assigned bus");
        }

        if (status != null && !status.isBlank()) {
            try {
                ComplaintStatus next = ComplaintStatus.valueOf(status.toUpperCase());
                validateTransition(complaint.getStatus(), next);
                complaint.setStatus(next);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid complaint status: " + status);
            }
        }

        if ((complaint.getStatus() == ComplaintStatus.RESOLVED || complaint.getStatus() == ComplaintStatus.CLOSED)
                && (response == null || response.isBlank())) {
            throw new BadRequestException("A resolution response is required before resolving or closing a complaint");
        }
        if (response != null) {
            if (response.length() > 1000) throw new BadRequestException("Complaint response cannot exceed 1000 characters");
            complaint.setResponse(response.trim());
        }

        complaint.setRespondedBy(respondedBy);
        complaint.setUpdatedAt(LocalDateTime.now());

        Complaint updated = complaintRepo.save(complaint);
        return ComplaintResponse.fromComplaint(updated);
    }

    private void validateTransition(ComplaintStatus current, ComplaintStatus next) {
        if (current == next) return;
        boolean allowed = switch (current) {
            case OPEN -> next == ComplaintStatus.IN_PROGRESS || next == ComplaintStatus.RESOLVED || next == ComplaintStatus.CLOSED;
            case IN_PROGRESS -> next == ComplaintStatus.RESOLVED || next == ComplaintStatus.CLOSED;
            case RESOLVED -> next == ComplaintStatus.CLOSED;
            case CLOSED -> false;
        };
        if (!allowed) throw new BadRequestException("Invalid complaint transition from " + current + " to " + next);
    }
}
