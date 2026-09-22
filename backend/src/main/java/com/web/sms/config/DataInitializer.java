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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Configuration
@ConditionalOnProperty(name = "btms.seed.enabled", havingValue = "true")
public class DataInitializer {

    @Value("${btms.seed.admin-password}")
    private String seedAdminPassword;

    @Value("${btms.seed.incharge-password}")
    private String seedInchargePassword;

    @Value("${btms.seed.student-password}")
    private String seedStudentPassword;

    @Bean
    public CommandLineRunner initData(AdminRepository adminRepo,
                                     InchargeRepository inchargeRepo,
                                     StudentRepository studentRepo,
                                     AcademicYearRepository academicYearRepo,
                                     PasswordEncoder passwordEncoder) {
        return args -> {
            if (seedAdminPassword == null || seedAdminPassword.isBlank()
                    || seedInchargePassword == null || seedInchargePassword.isBlank()
                    || seedStudentPassword == null || seedStudentPassword.isBlank()) {
                throw new IllegalStateException("All BTMS_SEED_*_PASSWORD environment variables are required when demo seeding is enabled");
            }
            // 1. Seed Default Admin if none exists
            if (!adminRepo.existsByAdminId("admin")) {
                Admin admin = new Admin();
                admin.setAdminId("admin");
                admin.setName("Chief Transport Administrator");
                admin.setEmail("transport@lbrce.ac.in");
                admin.setPassword(passwordEncoder.encode(seedAdminPassword));
                admin.setStatus("ACTIVE");
                admin.setCreatedAt(LocalDateTime.now());
                adminRepo.save(admin);
                System.out.println(">>> Initialized default Admin account");
            }

            // 2. Seed Default Incharge if none exists
            if (!inchargeRepo.existsByTeacherId("T1001")) {
                Incharge incharge = new Incharge();
                incharge.setTeacherId("T1001");
                incharge.setName("Dr. K. Srinivas Rao");
                incharge.setEmail("srinivas.k@lbrce.ac.in");
                incharge.setPhoneNumber("9876543210");
                incharge.setPassword(passwordEncoder.encode(seedInchargePassword));
                incharge.setDepartment("CSE");
                incharge.setDesignation("Associate Professor & Transport Incharge");
                incharge.setStatus("ACTIVE");
                incharge.setCreatedAt(LocalDateTime.now());
                inchargeRepo.save(incharge);
                System.out.println(">>> Initialized default In-Charge account");
            }

            // 3. Seed Default Student if none exists
            if (!studentRepo.existsByRollNumber("21761A0501")) {
                Student student = new Student();
                student.setRollNumber("21761A0501");
                student.setName("Sri Krishna");
                student.setEmail("21761a0501@lbrce.ac.in");
                student.setPhoneNumber("9123456780");
                student.setPassword(passwordEncoder.encode(seedStudentPassword));
                student.setBranch("CSE");
                student.setYear(4);
                student.setSemester(1);
                student.setGender("Male");
                student.setBloodGroup("O+");
                student.setAddress("Mylavaram, Krishna Dist.");
                student.setStatus("ACTIVE");
                student.setCreatedAt(LocalDateTime.now());
                studentRepo.save(student);
                System.out.println(">>> Initialized default Student account");
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
