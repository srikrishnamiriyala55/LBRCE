package com.web.sms.service;

import com.web.sms.dto.request.CreateInchargeRequest;
import com.web.sms.entity.Bus;
import com.web.sms.entity.Incharge;
import com.web.sms.exception.ResourceNotFoundException;
import com.web.sms.exception.BadRequestException;
import com.web.sms.repository.BusRepository;
import com.web.sms.repository.InchargeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InchargeServiceImpl implements InchargeService {

    private final InchargeRepository inchargeRepository;
    private final BusRepository busRepository;
    private final PasswordEncoder passwordEncoder;

    public InchargeServiceImpl(InchargeRepository inchargeRepository,
                               BusRepository busRepository,
                               PasswordEncoder passwordEncoder) {
        this.inchargeRepository = inchargeRepository;
        this.busRepository = busRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Incharge getInchargeById(long id) {
        return inchargeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incharge not found with id: " + id));
    }

    @Override
    public Incharge getInchargeByTeacherId(String teacherId) {
        return inchargeRepository.findByTeacherId(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Incharge not found with teacherId: " + teacherId));
    }

    @Override
    public List<Incharge> getAllIncharges() {
        return inchargeRepository.findAll();
    }

    @Override
    public Incharge createIncharge(CreateInchargeRequest req) {
        if(inchargeRepository.existsByTeacherId(req.getTeacherId().trim())) throw new BadRequestException("Teacher ID already exists");
        if(req.getEmail()!=null&&!req.getEmail().isBlank()&&inchargeRepository.findByEmail(req.getEmail().trim()).isPresent()) throw new BadRequestException("Email already exists");
        if(req.getPassword()==null||req.getPassword().length()<8) throw new BadRequestException("Password must contain at least 8 characters");
        Incharge incharge = new Incharge();
        incharge.setTeacherId(req.getTeacherId().trim().toUpperCase());
        incharge.setName(req.getName().trim());
        incharge.setEmail(req.getEmail().trim().toLowerCase());
        incharge.setPhoneNumber(req.getPhoneNumber().trim());
        incharge.setPassword(passwordEncoder.encode(req.getPassword()));
        incharge.setAddress(req.getAddress().trim());
        incharge.setDepartment(req.getDepartment().trim().toUpperCase());
        incharge.setDesignation(req.getDesignation().trim());
        incharge.setStatus("ACTIVE");
        LocalDateTime now=LocalDateTime.now();incharge.setCreatedAt(now);incharge.setUpdatedAt(now);
        return inchargeRepository.save(incharge);
    }

    @Override
    public Incharge updateIncharge(Long id, CreateInchargeRequest req) {
        Incharge incharge = inchargeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incharge not found with id: " + id));

        if (req.getEmail() != null && inchargeRepository.findByEmail(req.getEmail().trim().toLowerCase()).filter(other -> !other.getId().equals(id)).isPresent()) throw new BadRequestException("Email already exists");
        if (req.getName() != null) incharge.setName(req.getName().trim());
        if (req.getEmail() != null) incharge.setEmail(req.getEmail().trim().toLowerCase());
        if (req.getPhoneNumber() != null) incharge.setPhoneNumber(req.getPhoneNumber());
        if (req.getAddress() != null) incharge.setAddress(req.getAddress().trim());
        if (req.getDepartment() != null) incharge.setDepartment(req.getDepartment());
        if (req.getDesignation() != null) incharge.setDesignation(req.getDesignation());
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            if(req.getPassword().length()<8||req.getPassword().length()>72)throw new BadRequestException("Password must contain 8 to 72 characters");
            incharge.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        incharge.setUpdatedAt(LocalDateTime.now());
        return inchargeRepository.save(incharge);
    }

    @Override
    public Bus getAssignedBus(Long inchargeId) {
        List<Bus> buses = busRepository.findByInchargeId(inchargeId);
        return buses.isEmpty() ? null : buses.get(0);
    }
}
