package com.web.sms.config;

import com.web.sms.entity.AcademicYear;
import com.web.sms.entity.Admin;
import com.web.sms.entity.Incharge;
import com.web.sms.entity.Student;
import com.web.sms.repository.AcademicYearRepository;
import com.web.sms.repository.AdminRepository;
import com.web.sms.repository.InchargeRepository;
import com.web.sms.repository.StudentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Configuration
@ConditionalOnProperty(name = "btms.seed.enabled", havingValue = "true")
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(AdminRepository adminRepo,
                                     InchargeRepository inchargeRepo,
                                     StudentRepository studentRepo,
                                     AcademicYearRepository academicYearRepo,
                                     PasswordEncoder passwordEncoder) {
        return args -> {
            // 1. Seed Default Admin if none exists
            if (!adminRepo.existsByAdminId("admin")) {
                Admin admin = new Admin();
                admin.setAdminId("admin");
                admin.setName("Chief Transport Administrator");
                admin.setEmail("transport@lbrce.ac.in");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setStatus("ACTIVE");
                admin.setCreatedAt(LocalDateTime.now());
                adminRepo.save(admin);
                System.out.println(">>> Initialized default Admin: admin / admin123");
            }

            // 2. Seed Default Incharge if none exists
            if (!inchargeRepo.existsByTeacherId("T1001")) {
                Incharge incharge = new Incharge();
                incharge.setTeacherId("T1001");
                incharge.setName("Dr. K. Srinivas Rao");
                incharge.setEmail("srinivas.k@lbrce.ac.in");
                incharge.setPhoneNumber("9876543210");
                incharge.setPassword(passwordEncoder.encode("incharge123"));
                incharge.setDepartment("CSE");
                incharge.setDesignation("Associate Professor & Transport Incharge");
                incharge.setStatus("ACTIVE");
                incharge.setCreatedAt(LocalDateTime.now());
                inchargeRepo.save(incharge);
                System.out.println(">>> Initialized default Incharge: T1001 / incharge123");
            }

            // 3. Seed Default Student if none exists
            if (!studentRepo.existsByRollNumber("21761A0501")) {
                Student student = new Student();
                student.setRollNumber("21761A0501");
                student.setName("Sri Krishna");
                student.setEmail("21761a0501@lbrce.ac.in");
                student.setPhoneNumber("9123456780");
                student.setPassword(passwordEncoder.encode("student123"));
                student.setBranch("CSE");
                student.setYear(4);
                student.setSemester(1);
                student.setGender("Male");
                student.setBloodGroup("O+");
                student.setAddress("Mylavaram, Krishna Dist.");
                student.setStatus("ACTIVE");
                student.setCreatedAt(LocalDateTime.now());
                studentRepo.save(student);
                System.out.println(">>> Initialized default Student: 21761A0501 / student123");
            }

            // 4. Seed Default Active Academic Year
            if (academicYearRepo.findByActiveTrue().isEmpty()) {
                AcademicYear year = new AcademicYear();
                year.setYearName("2025-26");
                year.setStartDate(LocalDate.of(2025, 6, 1));
                year.setEndDate(LocalDate.of(2026, 5, 31));
                year.setActive(true);
                year.setCreatedAt(LocalDateTime.now());
                academicYearRepo.save(year);
                System.out.println(">>> Initialized default Active Academic Year: 2025-26");
            }
        };
    }
}
