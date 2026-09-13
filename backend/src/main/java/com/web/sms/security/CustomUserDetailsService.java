package com.web.sms.security;

import com.web.sms.repository.AdminRepository;
import com.web.sms.repository.InchargeRepository;
import com.web.sms.repository.StudentRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final StudentRepository studentRepo;
    private final InchargeRepository inchargeRepo;
    private final AdminRepository adminRepo;

    public CustomUserDetailsService(StudentRepository studentRepo, InchargeRepository inchargeRepo, AdminRepository adminRepo) {
        this.studentRepo = studentRepo;
        this.inchargeRepo = inchargeRepo;
        this.adminRepo = adminRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        if (loginId == null) {
            throw new UsernameNotFoundException("Login ID cannot be null");
        }
        String id = loginId.trim();
        return studentRepo.findByRollNumber(id)
                .or(() -> studentRepo.findByRollNumber(id.toUpperCase()))
                .map(UserPrincipal::fromStudent)
                .orElseGet(() -> inchargeRepo.findByTeacherId(id)
                .or(() -> inchargeRepo.findByTeacherId(id.toUpperCase()))
                .map(UserPrincipal::fromIncharge)
                .orElseGet(() -> adminRepo.findByAdminId(id)
                .or(() -> adminRepo.findByAdminId(id.toLowerCase()))
                .map(UserPrincipal::fromAdmin)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with loginId: " + loginId))));
    }
}
