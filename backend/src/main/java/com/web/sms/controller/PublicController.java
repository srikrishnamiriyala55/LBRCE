package com.web.sms.controller;

import com.web.sms.dto.response.PassResponse;
import com.web.sms.repository.AcademicYearRepository;
import com.web.sms.service.PassService;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final PassService passService;
    private final JdbcTemplate jdbcTemplate;
    private final AcademicYearRepository academicYearRepository;

    public PublicController(PassService passService,
                            JdbcTemplate jdbcTemplate,
                            AcademicYearRepository academicYearRepository) {
        this.passService = passService;
        this.jdbcTemplate = jdbcTemplate;
        this.academicYearRepository = academicYearRepository;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "LBRCE BTMS");
        body.put("timestamp", Instant.now().toString());
        try {
            jdbcTemplate.queryForObject("select 1", Integer.class);
            body.put("status", "UP");
            body.put("database", "UP");
            body.put("activeAcademicYear", academicYearRepository.findByActiveTrue()
                    .map(year -> year.getYearName()).orElse("NOT_CONFIGURED"));
            return ResponseEntity.ok(body);
        } catch (Exception failure) {
            body.put("status", "DOWN");
            body.put("database", "DOWN");
            return ResponseEntity.status(503).body(body);
        }
    }

    @GetMapping("/verify-pass")
    public PassResponse verifyPass(@RequestParam String token) {
        PassResponse response = passService.verifyPass(token);
        // Phone numbers are included on the student's private pass, never in public verification.
        response.setPhoneNumber(null);
        return response;
    }

    /**
     * Returns the student photo for a given pass verification token.
     * This endpoint is public so that the QR-scan verification page can display the photo
     * without requiring the viewer to be logged in.
     */
    @GetMapping("/pass-photo")
    public ResponseEntity<byte[]> passPhotoByToken(@RequestParam String token) {
        return passService.passPhotoByToken(token);
    }
}
